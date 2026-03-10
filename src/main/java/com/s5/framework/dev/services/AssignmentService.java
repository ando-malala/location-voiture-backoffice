package com.s5.framework.dev.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Planning;
import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.models.Vehicule;
import com.s5.framework.dev.repositories.HostelRepository;
import com.s5.framework.dev.repositories.VehiculeRepository;

/**
 * Service de simulation de planification.
 *
 * <h3>Règles d'assignation — résumé</h3>
 * <ol>
 *   <li>Les réservations sont triées par date-heure croissante, puis regroupées
 *       par créneau identique ({@code dateHeure}).</li>
 *   <li>À l'intérieur d'un groupe (même créneau), les réservations sont triées par
 *       {@code nbPassager} <strong>décroissant</strong> : la plus grande est traitée
 *       en priorité.</li>
 *   <li>On tente de constituer un <em>trajet combiné</em> :
 *       <ul>
 *         <li>On cherche le véhicule le plus petit dont la capacité ≥ total des passagers
 *             du groupe courant (commençant par la plus grande réservation).</li>
 *         <li>Si la capacité restante après la première réservation couvre les suivantes,
 *             elles sont ajoutées au même trajet.</li>
 *         <li>Sinon, chaque réservation non combinée est tentée séparément.</li>
 *       </ul>
 *   </li>
 *   <li>Sélection du véhicule : capacité minimale suffisante → priorité Diesel → aléatoire.</li>
 *   <li>Un véhicule déjà utilisé dans la simulation ne peut plus être assigné.</li>
 *   <li>Pour un trajet combiné, la voiture visite les hôtels <strong>du plus proche
 *       au plus éloigné</strong> de l'aéroport ; le temps de retour est calculé sur
 *       l'intégralité du trajet multi-stops.</li>
 * </ol>
 *
 * Aucune donnée n'est écrite en base.
 */
@Service
public class AssignmentService {

    private final VehiculeRepository vehiculeRepository;
    private final HostelRepository hostelRepository;
    private final DistanceService distanceService;
    private final ParametreService parametreService;
    private final ReservationService reservationService;

    @Autowired
    public AssignmentService(VehiculeRepository vehiculeRepository,
                             HostelRepository hostelRepository,
                             DistanceService distanceService,
                             ParametreService parametreService,
                             ReservationService reservationService) {
        this.vehiculeRepository = vehiculeRepository;
        this.hostelRepository = hostelRepository;
        this.distanceService = distanceService;
        this.parametreService = parametreService;
        this.reservationService = reservationService;
    }

    // ------------------------------------------------------------------ //
    //  Public types                                                        //
    // ------------------------------------------------------------------ //

    /** Résultat de la simulation pour une date donnée. */
    public static class SimulationResult {
        public final List<Planning> assigned;
        public final List<Long> unassignedIds;

        public SimulationResult(List<Planning> assigned, List<Long> unassignedIds) {
            this.assigned = assigned;
            this.unassignedIds = unassignedIds;
        }
    }

    // ------------------------------------------------------------------ //
    //  Main entry-point                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Simule l'assignation de véhicules pour toutes les réservations d'une date donnée.
     */
    public SimulationResult simuler(LocalDate date) {

        /* 1 — Récupérer les réservations du jour, triées par heure */
        List<Reservation> reservations = reservationService.findByDate(date)
                .stream()
                .sorted(Comparator.comparing(Reservation::getDateHeure))
                .collect(Collectors.toList());

        /* 2 — Paramètres */
        int vitesseMoyenne = parametreService.getVitesseMoyenne(); // km/h
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();
        Hostel aeroport = hostelRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Hôtel aéroport (id=1) non trouvé en base."));

        /* 3 — Regrouper par créneau (dateHeure identique) en conservant l'ordre */
        Map<LocalDateTime, List<Reservation>> groupes = reservations.stream()
                .collect(Collectors.groupingBy(
                        Reservation::getDateHeure,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Planning> assigned = new ArrayList<>();
        List<Long> unassignedIds = new ArrayList<>();
        Set<Long> vehiculesUtilises = new HashSet<>();
        Random random = new Random();

        /* 4 — Traiter chaque créneau */
        for (Map.Entry<LocalDateTime, List<Reservation>> entry : groupes.entrySet()) {
            LocalDateTime creneau = entry.getKey();

            /* Trier par nbPassager décroissant : priorité au plus grand groupe */
            List<Reservation> groupe = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(Reservation::getNbPassager).reversed())
                    .collect(Collectors.toList());

            Set<Long> dejaPrisEnCharge = new HashSet<>();

            for (int i = 0; i < groupe.size(); i++) {
                Reservation principale = groupe.get(i);
                if (dejaPrisEnCharge.contains(principale.getId())) continue;

                /* -- Tenter un trajet combiné avec les réservations restantes du groupe -- */
                // Construire la liste candidate : principale + suivantes non encore prises
                List<Reservation> candidatesTrajet = new ArrayList<>();
                candidatesTrajet.add(principale);
                for (int j = i + 1; j < groupe.size(); j++) {
                    if (!dejaPrisEnCharge.contains(groupe.get(j).getId())) {
                        candidatesTrajet.add(groupe.get(j));
                    }
                }

                /* Essayer d'abord le trajet combiné maximal, puis réduire si nécessaire */
                List<Reservation> trajet = tenteCombine(
                        candidatesTrajet, tousVehicules, vehiculesUtilises, random);

                if (trajet == null) {
                    // Aucun véhicule même pour la seule réservation principale
                    unassignedIds.add(principale.getId());
                    dejaPrisEnCharge.add(principale.getId());
                    continue;
                }

                /* Choisir le véhicule pour ce trajet */
                int totalPax = trajet.stream().mapToInt(Reservation::getNbPassager).sum();
                Vehicule choisi = choisirVehicule(tousVehicules, vehiculesUtilises, totalPax, random);
                // choisirVehicule ne peut pas être null car tenteCombine l'a validé
                vehiculesUtilises.add(choisi.getId());

                /* Construire la route : hotels triés par distance croissante depuis l'aéroport */
                List<Reservation> routeOrdonnee = trajet.stream()
                        .sorted(Comparator.comparingDouble(r ->
                                distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                        .collect(Collectors.toList());

                /* Calculer le temps du trajet multi-stops */
                long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
                LocalDateTime retour = creneau.plusMinutes(tempsTrajetMin);

                /* Noms des hôtels dans l'ordre de visite (pour affichage) */
                List<String> routeHotels = routeOrdonnee.stream()
                        .map(r -> r.getHotel().getNom())
                        .collect(Collectors.toList());
                boolean combined = trajet.size() > 1;

                /* Construire les ResInfo dans l'ordre de visite */
                List<Planning.ResInfo> resInfos = routeOrdonnee.stream()
                        .map(r -> new Planning.ResInfo(
                                r.getId(),
                                r.getNbPassager(),
                                r.getHotel().getNom(),
                                distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                        .collect(Collectors.toList());

                /* Un seul Planning par trajet (qu'il soit simple ou combiné) */
                Planning row = new Planning(resInfos, creneau, retour, choisi, combined, routeHotels);
                assigned.add(row);

                /* Marquer toutes les réservations du trajet comme traitées */
                for (Reservation res : trajet) {
                    dejaPrisEnCharge.add(res.getId());
                }
            }
        }

        return new SimulationResult(assigned, unassignedIds);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Tente de constituer le plus grand trajet combiné possible à partir de
     * {@code candidates} (déjà triés par nbPassager desc) avec les véhicules
     * disponibles. Retourne la liste des réservations combinées, ou {@code null}
     * si même la seule réservation tête ne peut être prise en charge.
     */
    private List<Reservation> tenteCombine(List<Reservation> candidates,
                                            List<Vehicule> tousVehicules,
                                            Set<Long> vehiculesUtilises,
                                            Random random) {
        /* Essayer du plus grand sous-ensemble au plus petit (greedy décroissant) */
        for (int taille = candidates.size(); taille >= 1; taille--) {
            List<Reservation> sous = candidates.subList(0, taille);
            int total = sous.stream().mapToInt(Reservation::getNbPassager).sum();
            if (choisirVehicule(tousVehicules, vehiculesUtilises, total, random) != null) {
                return new ArrayList<>(sous);
            }
        }
        return null; // aucun véhicule disponible
    }

    /**
     * Choisit le meilleur véhicule disponible pour {@code nbPassager} passagers.
     * Critères : capacité minimale suffisante → priorité Diesel → aléatoire.
     * Retourne {@code null} si aucun véhicule valide n'est disponible.
     */
    private Vehicule choisirVehicule(List<Vehicule> tousVehicules,
                                     Set<Long> vehiculesUtilises,
                                     int nbPassager,
                                     Random random) {
        List<Vehicule> candidats = tousVehicules.stream()
                .filter(v -> !vehiculesUtilises.contains(v.getId()))
                .filter(v -> v.getCapacite() >= nbPassager)
                .collect(Collectors.toList());

        if (candidats.isEmpty()) return null;

        int minCap = candidats.stream().mapToInt(Vehicule::getCapacite).min().getAsInt();
        List<Vehicule> plusPetits = candidats.stream()
                .filter(v -> v.getCapacite() == minCap)
                .collect(Collectors.toList());

        if (plusPetits.size() == 1) return plusPetits.get(0);

        List<Vehicule> diesels = plusPetits.stream()
                .filter(v -> "Diesel".equalsIgnoreCase(v.getTypeCarburant().getLibelle()))
                .collect(Collectors.toList());

        if (!diesels.isEmpty()) return diesels.get(random.nextInt(diesels.size()));
        return plusPetits.get(random.nextInt(plusPetits.size()));
    }

    /**
     * Calcule le temps total (en minutes) d'un trajet multi-stops.
     * Route : Aéroport → hotel_1 → hotel_2 → … → hotel_N → Aéroport
     * ({@code routeOrdonnee} est déjà triée du plus proche au plus éloigné).
     */
    private long calculerTempsTrajet(List<Reservation> routeOrdonnee,
                                     Hostel aeroport,
                                     int vitesseMoyenne) {
        if (routeOrdonnee.isEmpty()) return 0;

        double distanceTotale = 0;
        long idDernier = aeroport.getId();

        for (Reservation res : routeOrdonnee) {
            long idHotel = res.getHotel().getId();
            distanceTotale += distanceService.getDistanceKm(idDernier, idHotel);
            idDernier = idHotel;
        }
        // Retour vers l'aéroport depuis le dernier hôtel
        distanceTotale += distanceService.getDistanceKm(idDernier, aeroport.getId());

        return Math.round((distanceTotale / vitesseMoyenne) * 60);
    }
}

