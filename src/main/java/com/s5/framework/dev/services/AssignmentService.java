package com.s5.framework.dev.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Lieu;
import com.s5.framework.dev.models.Planning;
import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.models.Vehicule;
import com.s5.framework.dev.repositories.LieuRepository;
import com.s5.framework.dev.repositories.PlanningRepository;
import com.s5.framework.dev.repositories.VehiculeRepository;

/**
 * Service d'assignation automatique de véhicule(s) à une réservation.
 *
 * Logique :
 * 1. Sélection du véhicule : capacité >= nbPassager, plus petite capacité restante,
 *    puis priorité au Diesel en cas d'égalité, disponible sur le créneau.
 *    Si aucun véhicule seul ne suffit → répartition sur plusieurs véhicules.
 * 2. Calcul des horaires :
 *    - Départ = dateHeureDepart choisie par l'utilisateur
 *    - Arrivée hôtel = départ + (distance aéroport→hôtel / vitesse moyenne)
 *    - Départ hôtel = arrivée hôtel + temps d'attente
 *    - Retour aéroport = départ hôtel + (distance hôtel→aéroport / vitesse moyenne)
 * 3. Une réservation peut avoir plusieurs assignations (plannings).
 */
@Service
@Transactional
public class AssignmentService {

    private final VehiculeRepository vehiculeRepository;
    private final PlanningRepository planningRepository;
    private final LieuRepository lieuRepository;
    private final DistanceService distanceService;
    private final ParametreService parametreService;

    @Autowired
    public AssignmentService(VehiculeRepository vehiculeRepository,
                             PlanningRepository planningRepository,
                             LieuRepository lieuRepository,
                             DistanceService distanceService,
                             ParametreService parametreService) {
        this.vehiculeRepository = vehiculeRepository;
        this.planningRepository = planningRepository;
        this.lieuRepository = lieuRepository;
        this.distanceService = distanceService;
        this.parametreService = parametreService;
    }

    /**
     * Assigne automatiquement un ou plusieurs véhicules et crée les plannings.
     * Si le nombre de passagers dépasse la capacité du plus grand véhicule disponible,
     * la réservation est répartie sur plusieurs véhicules.
     * Une réservation peut être assignée plusieurs fois (plusieurs départs).
     *
     * @param reservation     la réservation à assigner
     * @param dateHeureDepart la date/heure de départ souhaitée depuis l'aéroport
     * @return la liste des Plannings créés
     */
    public List<Planning> assignerAutomatiquement(Reservation reservation, LocalDateTime dateHeureDepart) {
        int nbPassagers = reservation.getNbPassager();
        Hostel hotel = reservation.getHotel();

        // Récupérer le lieu Aéroport (code = 'AER')
        Lieu aeroport = lieuRepository.findByCode("AER")
                .orElseThrow(() -> new RuntimeException("Lieu 'AER' (Aéroport) non trouvé en base."));

        Lieu lieuHotel = hotel.getLieu();

        // --- Calcul des horaires ---
        int vitesseMoyenne = parametreService.getVitesseMoyenne();      // km/h
        int tempsAttente = parametreService.getTempsAttente();            // minutes

        // Distance aller : aéroport → lieu de l'hôtel
        double distanceAller = distanceService.getDistanceKm(aeroport.getId(), lieuHotel.getId());
        long tempsTrajetAllerMinutes = Math.round((distanceAller / vitesseMoyenne) * 60);

        // Distance retour : lieu de l'hôtel → aéroport
        double distanceRetour = distanceService.getDistanceKm(lieuHotel.getId(), aeroport.getId());
        long tempsTrajetRetourMinutes = Math.round((distanceRetour / vitesseMoyenne) * 60);

        LocalDateTime heureArriveeHotel = dateHeureDepart.plusMinutes(tempsTrajetAllerMinutes);
        LocalDateTime heureDepartHotel = heureArriveeHotel.plusMinutes(tempsAttente);
        LocalDateTime dateHeureRetour = heureDepartHotel.plusMinutes(tempsTrajetRetourMinutes);

        // --- Sélection automatique du/des véhicule(s) ---
        List<Vehicule> vehiculesChoisis = choisirVehicules(nbPassagers, dateHeureDepart, dateHeureRetour);

        // --- Création des Plannings avec répartition des passagers ---
        List<Planning> plannings = new ArrayList<>();
        int passagersRestants = nbPassagers;

        for (Vehicule vehicule : vehiculesChoisis) {
            int passagersDansCeVehicule = Math.min(passagersRestants, vehicule.getCapacite());
            passagersRestants -= passagersDansCeVehicule;

            Planning planning = new Planning();
            planning.setVehicule(vehicule);
            planning.setHotel(hotel);
            planning.setLieuDepart(aeroport);
            planning.setLieuRetour(aeroport);
            planning.setDateHeureDepart(dateHeureDepart);
            planning.setHeureArriveeHotel(heureArriveeHotel);
            planning.setHeureDepartHotel(heureDepartHotel);
            planning.setDateHeureRetour(dateHeureRetour);
            planning.setReservation(reservation);
            planning.setNbPassagers(passagersDansCeVehicule);
            planning.setStatut("PLANIFIE");
            plannings.add(planningRepository.save(planning));
        }

        return plannings;
    }

    /**
     * Choisit le(s) meilleur(s) véhicule(s) selon les critères :
     * 1. Essayer un seul véhicule avec capacité >= nbPassagers (plus petite capacité, diesel prioritaire)
     * 2. Si aucun véhicule seul ne suffit → répartir sur plusieurs véhicules :
     *    - Prendre le plus grand véhicule disponible, soustraire sa capacité, recommencer
     *    - Jusqu'à couvrir tous les passagers
     */
    private List<Vehicule> choisirVehicules(int nbPassagers, LocalDateTime depart, LocalDateTime retour) {
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();

        // Tous les véhicules disponibles sur le créneau
        List<Vehicule> disponibles = tousVehicules.stream()
                .filter(v -> planningRepository.findOverlapping(v.getId(), depart, retour).isEmpty())
                .collect(Collectors.toList());

        if (disponibles.isEmpty()) {
            throw new RuntimeException("Aucun véhicule disponible sur le créneau "
                    + depart + " → " + retour + ".");
        }

        // --- Tentative 1 : un seul véhicule suffit ---
        List<Vehicule> candidatsUniques = disponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassagers)
                .sorted(Comparator
                        .comparingInt(Vehicule::getCapacite)
                        .thenComparing((Vehicule v) -> dieselPriority(v)))
                .collect(Collectors.toList());

        if (!candidatsUniques.isEmpty()) {
            // Un seul véhicule suffit
            List<Vehicule> result = new ArrayList<>();
            result.add(candidatsUniques.get(0));
            return result;
        }

        // --- Tentative 2 : répartition sur plusieurs véhicules ---
        // Trier par capacité décroissante (plus grands d'abord) puis diesel en priorité
        List<Vehicule> triDescCapacite = disponibles.stream()
                .sorted(Comparator
                        .comparingInt(Vehicule::getCapacite).reversed()
                        .thenComparing((Vehicule v) -> dieselPriority(v)))
                .collect(Collectors.toList());

        List<Vehicule> vehiculesChoisis = new ArrayList<>();
        int passagersRestants = nbPassagers;

        for (Vehicule v : triDescCapacite) {
            if (passagersRestants <= 0) break;
            vehiculesChoisis.add(v);
            passagersRestants -= v.getCapacite();
        }

        if (passagersRestants > 0) {
            int capaciteTotale = disponibles.stream().mapToInt(Vehicule::getCapacite).sum();
            throw new RuntimeException("Capacité totale insuffisante : " + capaciteTotale
                    + " places disponibles pour " + nbPassagers + " passagers sur le créneau "
                    + depart + " → " + retour + ".");
        }

        // Optimisation finale : pour le dernier véhicule, choisir le plus petit qui suffit
        // pour les passagers restants dans ce dernier véhicule
        if (vehiculesChoisis.size() > 1) {
            // Recalculer les passagers couverts par tous sauf le dernier
            int couvertsSansDernier = 0;
            for (int i = 0; i < vehiculesChoisis.size() - 1; i++) {
                couvertsSansDernier += vehiculesChoisis.get(i).getCapacite();
            }
            int resteACouvrir = nbPassagers - couvertsSansDernier;
            Vehicule dernierChoisi = vehiculesChoisis.get(vehiculesChoisis.size() - 1);

            // Chercher un véhicule plus petit qui suffit et qui n'est pas déjà choisi
            List<Long> idsDejaChoisis = vehiculesChoisis.stream()
                    .map(Vehicule::getId).collect(Collectors.toList());

            disponibles.stream()
                    .filter(v -> !idsDejaChoisis.contains(v.getId()) || v.getId().equals(dernierChoisi.getId()))
                    .filter(v -> v.getCapacite() >= resteACouvrir)
                    .sorted(Comparator
                            .comparingInt(Vehicule::getCapacite)
                            .thenComparing((Vehicule v) -> dieselPriority(v)))
                    .findFirst()
                    .ifPresent(meilleur -> {
                        if (meilleur.getCapacite() < dernierChoisi.getCapacite()
                                || meilleur.getId().equals(dernierChoisi.getId())) {
                            // Ne remplacer que si c'est un meilleur choix
                            if (meilleur.getCapacite() < dernierChoisi.getCapacite()) {
                                vehiculesChoisis.set(vehiculesChoisis.size() - 1, meilleur);
                            }
                        }
                    });
        }

        return vehiculesChoisis;
    }

    /**
     * Retourne 0 pour Diesel (prioritaire), 1 sinon.
     */
    private int dieselPriority(Vehicule v) {
        String type = v.getTypeCarburant().getLibelle();
        return "Diesel".equalsIgnoreCase(type) ? 0 : 1;
    }
}
