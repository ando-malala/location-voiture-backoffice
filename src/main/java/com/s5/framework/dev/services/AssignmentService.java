package com.s5.framework.dev.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Planification;
import com.s5.framework.dev.models.PlanificationNonAssigne;
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
 *   <li>Les réservations sont triées par date-heure croissante.</li>
 *   <li>Une réservation peut attendre jusqu'à un certain temps (paramètre "Temps d attente")
 *       pour être regroupée avec les réservations ultérieures. Un trajet part à l'heure de la
 *       dernière réservation regroupée.</li>
 *   <li>Assignation individuelle en priorité :
 *       <ul>
 *         <li>Pour chaque réservation la plus ancienne (la plus urgente), on cherche le véhicule
 *             disponible dont la capacité est la <strong>plus proche</strong> (≥) du nombre de
 *             passagers de cette réservation seule.</li>
 *         <li>Une fois ce véhicule identifié, on remplit ses places restantes avec les
 *             réservations suivantes (dans les limites du temps d'attente) en privilégiant
 *             les groupes les plus grands.</li>
 *         <li>Si aucun véhicule n'est disponible pour une réservation, elle est marquée
 *             non assignée.</li>
 *       </ul>
 *   </li>
 *   <li>Sélection du véhicule : capacité minimale suffisante → priorité Diesel → aléatoire.</li>
 *   <li>Un véhicule peut être réutilisé après son heure de retour ; on priorise ceux
 *       qui ont effectué le moins de trajets.</li>
 *   <li>Pour un trajet combiné, la voiture visite les hôtels <strong>du plus proche
 *       au plus éloigné</strong> de l'aéroport ; le temps de retour est calculé sur
 *       l'intégralité du trajet multi-stops.</li>
 * </ol>
 *
 * Les planifications sont désormais persistées en base.
 * Lors d'une replanification pour une date donnée, les anciennes planifications
 * pour cette date sont supprimées puis recalculées.
 */
@Service
public class AssignmentService {

    private final VehiculeRepository vehiculeRepository;
    private final HostelRepository hostelRepository;
    private final DistanceService distanceService;
    private final ParametreService parametreService;
    private final ReservationService reservationService;
    private final PlanificationService planificationService;

    @Autowired
    public AssignmentService(VehiculeRepository vehiculeRepository,
                             HostelRepository hostelRepository,
                             DistanceService distanceService,
                             ParametreService parametreService,
                             ReservationService reservationService,
                             PlanificationService planificationService) {
        this.vehiculeRepository = vehiculeRepository;
        this.hostelRepository = hostelRepository;
        this.distanceService = distanceService;
        this.parametreService = parametreService;
        this.reservationService = reservationService;
        this.planificationService = planificationService;
    }

    // ------------------------------------------------------------------ //
    //  Public types                                                        //
    // ------------------------------------------------------------------ //

    /** Résultat de la simulation pour une date donnée. */
    public static class SimulationResult {
        public final List<Planning> assigned;
        public final List<Reservation> unassigned;

        public SimulationResult(List<Planning> assigned, List<Reservation> unassigned) {
            this.assigned = assigned;
            this.unassigned = unassigned;
        }
    }

    /** Résultat intermédiaire de tenteCombine : trajet retenu + véhicule déjà sélectionné. */
    private static class TrajetAssigne {
        final List<Reservation> trajet;
        final Vehicule vehicule;

        TrajetAssigne(List<Reservation> trajet, Vehicule vehicule) {
            this.trajet = trajet;
            this.vehicule = vehicule;
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

        /* 3 — Temps d'attente : on peut regrouper des réservations proches dans le temps
         * tant que la différence entre la plus ancienne et la plus récente reste <= temps d'attente. */
        int tempsAttente = parametreService.getTempsAttente(); // en minutes

        /* 4 — Réinitialiser les planifications existantes pour cette date (replanification) */
        planificationService.clearForDate(date);

        List<Planning> assigned = new ArrayList<>();
        List<Reservation> unassigned = new ArrayList<>();

        List<Planification> planifications = new ArrayList<>();
        List<PlanificationNonAssigne> nonAssignes = new ArrayList<>();

        Map<Long, LocalDateTime> vehiculeDisponible = new HashMap<>();
        Map<Long, Integer> vehiculeNbTrajets = new HashMap<>();
        LocalDateTime debutJournee = date.atStartOfDay();
        for (Vehicule v : tousVehicules) {
            vehiculeDisponible.put(v.getId(), debutJournee);
            vehiculeNbTrajets.put(v.getId(), 0);
        }

        Random random = new Random();

        /* 5 — Traiter les réservations en prenant en compte le temps d'attente. */
        List<Reservation> pending = new ArrayList<>(reservations);

        while (!pending.isEmpty()) {
            // Toujours commencer par la réservation la plus ancienne (celle qui risque
            // le plus de dépasser son temps d'attente).
            Reservation principale = pending.get(0);
            LocalDateTime maxDepart = principale.getDateHeure().plusMinutes(tempsAttente);

            /* Les candidats au regroupement sont celles dont l'heure est <= maxDepart */
            List<Reservation> candidats = pending.stream()
                    .filter(r -> !r.getDateHeure().isAfter(maxDepart))
                    .collect(Collectors.toList());

            /* Construire la liste pour l'algorithme de combinaison :
             * - Principale en premier, puis les autres candidats triés par nbPassager décroissant. */
            List<Reservation> candidatesTrajet = new ArrayList<>();
            candidatesTrajet.add(principale);
            candidatesTrajet.addAll(candidats.stream()
                    .filter(r -> !r.getId().equals(principale.getId()))
                    .sorted(Comparator.comparingInt(Reservation::getNbPassager).reversed()
                            .thenComparing(Reservation::getDateHeure))
                    .collect(Collectors.toList()));

            TrajetAssigne resultat = tenteCombine(
                    candidatesTrajet, tousVehicules,
                    vehiculeDisponible, vehiculeNbTrajets,
                    maxDepart, random);

            if (resultat == null) {
                // Aucun véhicule même pour la seule réservation principale (ou pas disponible)
                unassigned.add(principale);
                nonAssignes.add(new PlanificationNonAssigne(
                        date,
                        principale,
                        "Aucun véhicule avec capacité ≥ " + principale.getNbPassager() + " pl. disponible"));
                pending.remove(0);
                continue;
            }

            List<Reservation> trajet = resultat.trajet;
            Vehicule choisi = resultat.vehicule;

            /* Départ effectif = heure de la dernière réservation du trajet */
            LocalDateTime depart = trajet.stream()
                    .map(Reservation::getDateHeure)
                    .max(LocalDateTime::compareTo)
                    .orElse(principale.getDateHeure());

            /* Construire la route : hôtels triés par distance croissante depuis l'aéroport */
            List<Reservation> routeOrdonnee = trajet.stream()
                    .sorted(Comparator.comparingDouble(r ->
                            distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                    .collect(Collectors.toList());

            /* Calculer le temps du trajet multi-stops */
            long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
            LocalDateTime retour = depart.plusMinutes(tempsTrajetMin);

            /* Mettre à jour l'état du véhicule (disponibilité + nombre de trajets) */
            vehiculeNbTrajets.compute(choisi.getId(), (k, v) -> v == null ? 1 : v + 1);
            vehiculeDisponible.put(choisi.getId(), retour);

            /* Noms des hôtels dans l'ordre de visite (pour affichage) */
            List<String> routeHotels = routeOrdonnee.stream()
                    .map(r -> r.getHotel().getNom())
                    .collect(Collectors.toList());
            boolean combined = trajet.size() > 1;

            /* Construire les ResInfo dans l'ordre de visite */
            List<Planning.ResInfo> resInfos = routeOrdonnee.stream()
                    .map(r -> new Planning.ResInfo(
                            r.getId(),
                            r.getIdClient(),
                            r.getNbPassager(),
                            r.getHotel().getNom(),
                            distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                    .collect(Collectors.toList());

            /* Un seul Planning par trajet (qu'il soit simple ou combiné) */
            Planning row = new Planning(resInfos, depart, retour, choisi, combined, routeHotels);
            assigned.add(row);

            /* Persister cette planification */
            planifications.add(new Planification(date, depart, retour, choisi, combined,
                    String.join(" → ", routeHotels), trajet));

            /* Marquer toutes les réservations du trajet comme traitées */
            Set<Long> trajetIds = trajet.stream().map(Reservation::getId).collect(Collectors.toSet());
            pending.removeIf(r -> trajetIds.contains(r.getId()));
        }

        planificationService.savePlanifications(planifications);
        planificationService.saveNonAssignes(nonAssignes);

        return new SimulationResult(assigned, unassigned);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Tente d'assigner un trajet à partir de {@code candidates} (déjà triés par
     * nbPassager desc) avec les véhicules disponibles.
     *
     * <p>Règle : on cherche d'abord à servir la réservation principale seule avec
     * le véhicule dont la capacité est la plus proche (≥) de son nombre de passagers.
     * Une fois ce véhicule identifié, on remplit ses places restantes avec les
     * réservations suivantes du même créneau (les plus grandes d'abord) qui tiennent.
     * Retourne {@code null} si aucun véhicule n'est disponible pour la principale.
     */
    private TrajetAssigne tenteCombine(List<Reservation> candidates,
                                       List<Vehicule> tousVehicules,
                                       Map<Long, LocalDateTime> vehiculeDisponible,
                                       Map<Long, Integer> vehiculeNbTrajets,
                                       LocalDateTime depart,
                                       Random random) {
        Reservation principale = candidates.get(0);

        /* 1 — Chercher le véhicule le mieux adapté à la réservation principale seule */
        Vehicule vehiculePrincipal = choisirVehicule(
                tousVehicules, vehiculeDisponible, vehiculeNbTrajets, depart,
                principale.getNbPassager(), random);

        if (vehiculePrincipal == null) {
            return null; // aucun véhicule disponible, même pour la principale
        }

        /* 2 — Remplir les places restantes de ce véhicule avec d'autres réservations
         *     du même créneau (les plus grandes d'abord pour maximiser le remplissage) */
        List<Reservation> trajet = new ArrayList<>();
        trajet.add(principale);
        int placesRestantes = vehiculePrincipal.getCapacite() - principale.getNbPassager();

        /* Trier les suivantes par nbPassager décroissant : on place d'abord les groupes
         * les plus grands qui tiennent dans les places restantes, évitant ainsi qu'un
         * petit groupe "bouche" des places qu'un plus grand groupe aurait pu occuper. */
        List<Reservation> suivantes = candidates.subList(1, candidates.size())
                .stream()
                .sorted(Comparator.comparingInt(Reservation::getNbPassager).reversed())
                .collect(Collectors.toList());

        for (Reservation suivante : suivantes) {
            if (suivante.getNbPassager() <= placesRestantes) {
                trajet.add(suivante);
                placesRestantes -= suivante.getNbPassager();
            }
        }

        return new TrajetAssigne(trajet, vehiculePrincipal);
    }

    /**
     * Choisit le meilleur véhicule disponible pour {@code nbPassager} passagers.
     * Critères : capacité minimale suffisante → priorité Diesel → aléatoire.
     * Retourne {@code null} si aucun véhicule valide n'est disponible.
     */
    private Vehicule choisirVehicule(List<Vehicule> tousVehicules,
                                     Map<Long, LocalDateTime> vehiculeDisponible,
                                     Map<Long, Integer> vehiculeNbTrajets,
                                     LocalDateTime depart,
                                     int nbPassager,
                                     Random random) {
        List<Vehicule> candidats = tousVehicules.stream()
                .filter(v -> {
                    LocalDateTime dispo = vehiculeDisponible.getOrDefault(v.getId(), LocalDateTime.MIN);
                    return !dispo.isAfter(depart);
                })
                .filter(v -> v.getCapacite() >= nbPassager)
                .collect(Collectors.toList());

        if (candidats.isEmpty()) return null;

        int minTrajets = candidats.stream()
                .mapToInt(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0))
                .min()
                .orElse(0);
        List<Vehicule> meilleurs = candidats.stream()
                .filter(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0) == minTrajets)
                .collect(Collectors.toList());

        int minCap = meilleurs.stream().mapToInt(Vehicule::getCapacite).min().getAsInt();
        List<Vehicule> plusPetits = meilleurs.stream()
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
            // Si deux stops successifs sont au même hôtel, la distance est 0 (pas en BDD)
            if (idHotel != idDernier) {
                distanceTotale += distanceService.getDistanceKm(idDernier, idHotel);
            }
            idDernier = idHotel;
        }
        // Retour vers l'aéroport depuis le dernier hôtel
        if (idDernier != aeroport.getId()) {
            distanceTotale += distanceService.getDistanceKm(idDernier, aeroport.getId());
        }

        return Math.round((distanceTotale / vitesseMoyenne) * 60);
    }
}

