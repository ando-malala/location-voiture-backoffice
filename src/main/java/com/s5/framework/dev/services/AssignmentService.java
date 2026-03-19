package com.s5.framework.dev.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
        public final List<PlanificationNonAssigne> nonAssigned;

        public SimulationResult(List<Planning> assigned, List<PlanificationNonAssigne> nonAssigned) {
            this.assigned = assigned;
            this.nonAssigned = nonAssigned;
        }
    }

    /** Résultat intermédiaire de tenteCombine : trajet retenu + véhicule déjà sélectionné. */
    private static class TrajetAssigne {
        final List<ReservationSim> trajet;
        final Vehicule vehicule;
        final ReservationSim partialRemaining;

        TrajetAssigne(List<ReservationSim> trajet, Vehicule vehicule, ReservationSim partialRemaining) {
            this.trajet = trajet;
            this.vehicule = vehicule;
            this.partialRemaining = partialRemaining;
        }
    }

    /** Réservation simulée (peut être scindée) pour la planification. */
    private static class ReservationSim {
        final Reservation reservation;
        int remaining;

        ReservationSim(Reservation reservation) {
            this.reservation = reservation;
            this.remaining = reservation.getNbPassager();
        }

        ReservationSim(Reservation reservation, int remaining) {
            this.reservation = reservation;
            this.remaining = remaining;
        }

        public Long getId() {
            return reservation.getId();
        }

        public String getIdClient() {
            return reservation.getIdClient();
        }

        public int getNbPassager() {
            return remaining;
        }

        public LocalDateTime getDateHeure() {
            return reservation.getDateHeure();
        }

        public Hostel getHotel() {
            return reservation.getHotel();
        }

        public Reservation getReservation() {
            return reservation;
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
        List<ReservationSim> reservations = reservationService.findByDate(date)
                .stream()
                .sorted(Comparator.comparing(Reservation::getDateHeure))
                .map(ReservationSim::new)
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
        List<ReservationSim> pending = new ArrayList<>(reservations);

        while (!pending.isEmpty()) {
            // Toujours commencer par la réservation la plus ancienne (celle qui risque
            // le plus de dépasser son temps d'attente).
            ReservationSim principale = pending.get(0);
            LocalDateTime maxDepart = principale.getDateHeure().plusMinutes(tempsAttente);

            /* Les candidats au regroupement sont celles dont l'heure est <= maxDepart */
            List<ReservationSim> candidats = pending.stream()
                    .filter(r -> !r.getDateHeure().isAfter(maxDepart))
                    .collect(Collectors.toList());

            /* Construire la liste pour l'algorithme de combinaison :
             * - Principale en premier, puis les autres candidats triés par nbPassager décroissant. */
            List<ReservationSim> candidatesTrajet = new ArrayList<>();
            candidatesTrajet.add(principale);
            candidatesTrajet.addAll(candidats.stream()
                    .filter(r -> !r.getId().equals(principale.getId()))
                    .sorted(Comparator.comparingInt(ReservationSim::getNbPassager).reversed()
                            .thenComparing(ReservationSim::getDateHeure))
                    .collect(Collectors.toList()));

            // --- 5.1 — Si la réservation ne tient dans aucun véhicule seul, on la scinde. ---
            List<Vehicule> disponibles = tousVehicules.stream()
                    .filter(v -> !vehiculeDisponible.getOrDefault(v.getId(), debutJournee).isAfter(maxDepart))
                    .collect(Collectors.toList());

            int totalCapacite = disponibles.stream().mapToInt(Vehicule::getCapacite).sum();
            int maxCapaciteDispo = disponibles.stream().mapToInt(Vehicule::getCapacite).max().orElse(0);

            if (disponibles.isEmpty()) {
                // Aucun véhicule disponible à cette heure : réservation non assignable.
                nonAssignes.add(new PlanificationNonAssigne(
                        date,
                        principale.getReservation(),
                        principale.getNbPassager(),
                        "Aucun véhicule disponible à l'heure de départ"));
                pending.remove(0);
                continue;
            }

            if (principale.getNbPassager() > maxCapaciteDispo) {
                // Scinder la réservation en plusieurs véhicules avant de passer à la suivante.
                int reste = principale.getNbPassager();
                while (reste > 0) {
                    Vehicule choisi = choisirVehicule(
                            tousVehicules, vehiculeDisponible, vehiculeNbTrajets, maxDepart, reste, random);
                    if (choisi == null) {
                        // Aucun véhicule disponible pour le reste de la réservation : on marque le reste comme non assigné.
                        nonAssignes.add(new PlanificationNonAssigne(
                                date,
                                principale.getReservation(),
                                reste,
                                "Reste de la réservation non assigné (" + reste + " passagers)")
                        );
                        break;
                    }

                    int pris = Math.min(reste, choisi.getCapacite());
                    reste -= pris;

                    // Calcul du retour pour ce véhicule (trajet simple aller-retour)
                    LocalDateTime depart = maxDepart;
                    double distance = distanceService.getDistanceKm(aeroport.getId(), principale.getHotel().getId());
                    long tempsTrajetMin = Math.round((distance * 2 / vitesseMoyenne) * 60);
                    LocalDateTime retour = depart.plusMinutes(tempsTrajetMin);

                    // Mettre à jour l'état du véhicule
                    vehiculeNbTrajets.compute(choisi.getId(), (k, v) -> v == null ? 1 : v + 1);
                    vehiculeDisponible.put(choisi.getId(), retour);

                    // Enregistrer le planning (une ligne par véhicule utilisé)
                    List<Planning.ResInfo> resInfos = List.of(new Planning.ResInfo(
                            principale.getId(),
                            principale.getIdClient(),
                            pris,
                            principale.getHotel().getNom(),
                            distance));

                    Planning row = new Planning(resInfos, depart, retour, choisi, false,
                            List.of(principale.getHotel().getNom()));
                    assigned.add(row);

                    planifications.add(new Planification(date, depart, retour, choisi, false,
                            principale.getHotel().getNom(), List.of(principale.getReservation())));
                }

                pending.remove(0);
                continue;
            }

            // --- 5.2 — Réservation tient dans un seul véhicule : on essaye de combiner avec d'autres. ---
            TrajetAssigne resultat = tenteCombine(
                    candidatesTrajet, tousVehicules,
                    vehiculeDisponible, vehiculeNbTrajets,
                    maxDepart, random);

            if (resultat == null) {
                // Aucun véhicule même pour la seule réservation principale (ou pas disponible)
                nonAssignes.add(new PlanificationNonAssigne(
                        date,
                        principale.getReservation(),
                        principale.getNbPassager(),
                        "Aucun véhicule avec capacité ≥ " + principale.getNbPassager() + " pl. disponible"));
                pending.remove(0);
                continue;
            }

            List<ReservationSim> trajet = resultat.trajet;
            Vehicule choisi = resultat.vehicule;

            /* Départ effectif = heure de la dernière réservation du trajet */
            LocalDateTime depart = trajet.stream()
                    .map(ReservationSim::getDateHeure)
                    .max(LocalDateTime::compareTo)
                    .orElse(principale.getDateHeure());

            /* Construire la route : hôtels triés par distance croissante depuis l'aéroport */
            List<ReservationSim> routeOrdonnee = trajet.stream()
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
                    String.join(" → ", routeHotels),
                    trajet.stream().map(ReservationSim::getReservation).collect(Collectors.toList())));

            /* Marquer toutes les réservations du trajet comme traitées */
            pending.removeIf(r -> r.getNbPassager() == 0);

            /* Si une fragment de réservation a été partiellement utilisé, on le remet en tête */
            if (resultat.partialRemaining != null) {
                if (pending.remove(resultat.partialRemaining)) {
                    pending.add(0, resultat.partialRemaining);
                }
            }
        }

        planificationService.savePlanifications(planifications);
        planificationService.saveNonAssignes(nonAssignes);

        return new SimulationResult(assigned, nonAssignes);
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
    private TrajetAssigne tenteCombine(List<ReservationSim> candidates,
                                       List<Vehicule> tousVehicules,
                                       Map<Long, LocalDateTime> vehiculeDisponible,
                                       Map<Long, Integer> vehiculeNbTrajets,
                                       LocalDateTime depart,
                                       Random random) {
        ReservationSim principale = candidates.get(0);

        /* 1 — Chercher le véhicule le mieux adapté à la réservation principale seule */
        Vehicule vehiculePrincipal = choisirVehicule(
                tousVehicules, vehiculeDisponible, vehiculeNbTrajets, depart,
                principale.getNbPassager(), random);

        if (vehiculePrincipal == null) {
            return null; // aucun véhicule disponible, même pour la principale
        }

        /* 2 — Remplir le véhicule en utilisant au maximum les places disponibles. */
        List<ReservationSim> trajet = new ArrayList<>();
        trajet.add(principale);
        int placesRestantes = vehiculePrincipal.getCapacite() - principale.getNbPassager();
        principale.remaining = 0;

        /* Trier les suivantes par nbPassager décroissant pour maximiser l'utilisation. */
        List<ReservationSim> suivantes = candidates.subList(1, candidates.size())
                .stream()
                .filter(r -> r.getNbPassager() > 0)
                .sorted(Comparator.comparingInt(ReservationSim::getNbPassager).reversed())
                .collect(Collectors.toList());

        ReservationSim partialRemaining = null;

        for (ReservationSim suivante : suivantes) {
            if (placesRestantes <= 0) break;

            int demande = suivante.getNbPassager();
            if (demande <= placesRestantes) {
                trajet.add(suivante);
                placesRestantes -= demande;
                suivante.remaining = 0;
            } else {
                // On peut utiliser une partie de cette réservation pour combler les places restantes.
                ReservationSim part = new ReservationSim(suivante.getReservation(), placesRestantes);
                trajet.add(part);
                suivante.remaining = demande - placesRestantes;
                partialRemaining = suivante;
                placesRestantes = 0;
                break;
            }
        }

        return new TrajetAssigne(trajet, vehiculePrincipal, partialRemaining);
    }

    /**
     * Choisit le meilleur véhicule disponible pour {@code nbPassager} passagers.
     * <p>
     * Critères :
     * <ol>
     *   <li>est disponible à {@code depart} (retour déjà effectué)</li>
     *   <li>minimise le nombre de trajets déjà effectués</li>
     *   <li>si possible, choisit un véhicule dont la capacité est la plus proche (≥) de
     *       {@code nbPassager}</li>
     *   <li>si aucun véhicule n'est assez grand, choisit le plus grand disponible</li>
     *   <li>en cas d'égalité, préférence Diesel puis aléatoire</li>
     * </ol>
     * Retourne {@code null} si aucun véhicule n'est disponible.
     */
    private Vehicule choisirVehicule(List<Vehicule> tousVehicules,
                                     Map<Long, LocalDateTime> vehiculeDisponible,
                                     Map<Long, Integer> vehiculeNbTrajets,
                                     LocalDateTime depart,
                                     int nbPassager,
                                     Random random) {
        List<Vehicule> candidatsDisponibles = tousVehicules.stream()
                .filter(v -> {
                    LocalDateTime dispo = vehiculeDisponible.getOrDefault(v.getId(), LocalDateTime.MIN);
                    return !dispo.isAfter(depart);
                })
                .collect(Collectors.toList());

        if (candidatsDisponibles.isEmpty()) return null;

        // 1) Favoriser les véhicules dont la capacité est >= nbPassager
        List<Vehicule> candidats = candidatsDisponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassager)
                .collect(Collectors.toList());

        // 2) Si aucun véhicule n'est assez grand, on prend les véhicules les plus grands disponibles
        if (candidats.isEmpty()) {
            int maxCap = candidatsDisponibles.stream().mapToInt(Vehicule::getCapacite).max().orElse(0);
            candidats = candidatsDisponibles.stream()
                    .filter(v -> v.getCapacite() == maxCap)
                    .collect(Collectors.toList());
        }

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
    private long calculerTempsTrajet(List<ReservationSim> routeOrdonnee,
                                     Hostel aeroport,
                                     int vitesseMoyenne) {
        if (routeOrdonnee.isEmpty()) return 0;

        double distanceTotale = 0;
        long idDernier = aeroport.getId();

        for (ReservationSim res : routeOrdonnee) {
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

