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
 * Service de simulation de planification (algorithme batch par fenêtre).
 *
 * <h3>Règles d'assignation</h3>
 * <ol>
 *   <li>Les réservations sont triées par date-heure croissante.</li>
 *   <li>Une réservation peut attendre jusqu'au temps d'attente paramétré
 *       pour être regroupée avec les réservations ultérieures.</li>
 *   <li>Au sein d'une fenêtre, toutes les réservations sont assignées
 *       simultanément (batch) : les plus grosses d'abord.</li>
 *   <li>Un départ unique par batch = max(dernière arrivée client, dernière
 *       disponibilité véhicule utilisé).</li>
 *   <li>Sélection du véhicule : capacité minimale suffisante, moins de
 *       trajets effectués, priorité Diesel, puis aléatoire.</li>
 *   <li>Un véhicule peut être réutilisé après son heure de retour.</li>
 *   <li>Route multi-stops : hôtels du plus proche au plus éloigné de
 *       l'aéroport.</li>
 * </ol>
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

    // ------------------------------------------------------------------ //
    //  Inner types                                                         //
    // ------------------------------------------------------------------ //

    /** Réservation simulée (peut être scindée) pour la planification. */
    private static class ReservationSim {
        final Reservation reservation;
        int remaining;
        LocalDateTime dateHeureSimulee;

        ReservationSim(Reservation reservation) {
            this.reservation = reservation;
            this.remaining = reservation.getNbPassager();
            this.dateHeureSimulee = reservation.getDateHeure();
        }

        ReservationSim(ReservationSim source, int remaining) {
            this.reservation = source.reservation;
            this.remaining = remaining;
            this.dateHeureSimulee = source.dateHeureSimulee;
        }

        public Long getId()               { return reservation.getId(); }
        public String getIdClient()        { return reservation.getIdClient(); }
        public int getNbPassager()         { return remaining; }
        public LocalDateTime getDateHeure(){ return dateHeureSimulee; }
        public Hostel getHotel()           { return reservation.getHotel(); }
        public Reservation getReservation(){ return reservation; }

        public void setDateHeureSimulee(LocalDateTime dt) { this.dateHeureSimulee = dt; }
    }

    private static class Allocation {
        final ReservationSim reservation;
        final int quantite;

        Allocation(ReservationSim reservation, int quantite) {
            this.reservation = reservation;
            this.quantite = quantite;
        }
    }

    /** Trajet planifié dans un batch : liste d'allocations + véhicule assigné. */
    private static class BatchTrip {
        final List<ReservationSim> allocations;
        final Vehicule vehicule;

        BatchTrip(List<ReservationSim> allocations, Vehicule vehicule) {
            this.allocations = allocations;
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
        List<ReservationSim> reservations = reservationService.findByDate(date)
                .stream()
                .sorted(Comparator.comparing(Reservation::getDateHeure))
                .map(ReservationSim::new)
                .collect(Collectors.toList());

        /* 2 — Paramètres */
        int vitesseMoyenne = parametreService.getVitesseMoyenne();
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();
        Hostel aeroport = hostelRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Hôtel aéroport (id=1) non trouvé en base."));

        /* 3 — Temps d'attente */
        int tempsAttente = parametreService.getTempsAttente();

        /* 4 — Réinitialiser les planifications existantes pour cette date */
        planificationService.clearForDate(date);

        List<Planning> assigned = new ArrayList<>();
        List<Planification> planifications = new ArrayList<>();
        List<PlanificationNonAssigne> nonAssignes = new ArrayList<>();

        Map<Long, LocalDateTime> vehiculeDisponible = new HashMap<>();
        Map<Long, Integer> vehiculeNbTrajets = new HashMap<>();
        LocalDateTime debutJournee = date.atStartOfDay();
        for (Vehicule v : tousVehicules) {
            LocalDateTime disponibiliteInitiale = v.getHeureDispo() != null
                    ? date.atTime(v.getHeureDispo())
                    : debutJournee;
            vehiculeDisponible.put(v.getId(), disponibiliteInitiale);
            vehiculeNbTrajets.put(v.getId(), 0);
        }

        Random random = new Random();

        /* 5 — Traiter les réservations par batch (fenêtre de temps d'attente). */
        List<ReservationSim> pending = new ArrayList<>(reservations);

        while (!pending.isEmpty()) {
            // Fenêtre d'attente : plus ancienne réservation non traitée.
            LocalDateTime earliest = pending.stream()
                    .map(ReservationSim::getDateHeure)
                    .min(LocalDateTime::compareTo)
                    .orElse(debutJournee);
            LocalDateTime maxDepart = earliest.plusMinutes(tempsAttente);

            List<ReservationSim> candidats = pending.stream()
                    .filter(r -> !r.getDateHeure().isAfter(maxDepart))
                    .collect(Collectors.toList());

            if (candidats.isEmpty()) {
                break;
            }

            // Véhicules disponibles dans cette fenêtre.
            List<Vehicule> disponibles = tousVehicules.stream()
                    .filter(v -> !vehiculeDisponible.getOrDefault(v.getId(), debutJournee).isAfter(maxDepart))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                // Aucun véhicule dispo dans la fenêtre : décaler la réservation la plus ancienne.
                ReservationSim plusAncienne = candidats.stream()
                        .min(Comparator.comparing(ReservationSim::getDateHeure))
                        .orElse(candidats.get(0));

                LocalDateTime prochaineDisponibilite = vehiculeDisponible.values().stream()
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

                if (prochaineDisponibilite != null) {
                    LocalDateTime prochaineReservationFuture = pending.stream()
                            .filter(r -> r != plusAncienne)
                            .map(ReservationSim::getDateHeure)
                            .filter(h -> h.isAfter(plusAncienne.getDateHeure()))
                            .min(LocalDateTime::compareTo)
                            .orElse(null);

                    LocalDateTime nouveauCreneau = prochaineDisponibilite;
                    if (prochaineReservationFuture != null && prochaineReservationFuture.isAfter(nouveauCreneau)) {
                        nouveauCreneau = prochaineReservationFuture;
                    }
                    plusAncienne.setDateHeureSimulee(nouveauCreneau);
                    continue;
                }

                nonAssignes.add(new PlanificationNonAssigne(
                        date, plusAncienne.getReservation(), plusAncienne.getNbPassager(),
                        "Aucun véhicule disponible"));
                pending.remove(plusAncienne);
                continue;
            }

            // --- Traitement batch : assigner tous les candidats de la fenêtre ---

            // Trier les candidats par nbPassager décroissant (les plus gros d'abord).
            candidats.sort(Comparator.comparingInt(ReservationSim::getNbPassager).reversed()
                    .thenComparing(ReservationSim::getDateHeure));

            // Copie locale des véhicules disponibles pour ce batch.
            List<Vehicule> vehiculesBatch = new ArrayList<>(disponibles);

            // Stocker les trajets planifiés dans ce batch.
            List<BatchTrip> batchTrips = new ArrayList<>();

            for (int i = 0; i < candidats.size(); i++) {
                ReservationSim principale = candidats.get(i);
                if (principale.getNbPassager() <= 0) continue;

                // Chercher le meilleur véhicule pour cette réservation.
                Vehicule choisi = choisirVehiculeDansBatch(vehiculesBatch, vehiculeDisponible,
                        vehiculeNbTrajets, maxDepart, principale.getNbPassager(), random);

                if (choisi == null) {
                    // Pas de véhicule dans le batch, on la laisse pour le prochain batch.
                    continue;
                }

                int pris = Math.min(principale.getNbPassager(), choisi.getCapacite());
                int placesRestantes = choisi.getCapacite() - pris;

                List<ReservationSim> allocations = new ArrayList<>();
                allocations.add(new ReservationSim(principale, pris));
                principale.remaining -= pris;

                // Remplir les places restantes avec les plus petites réservations.
                if (placesRestantes > 0) {
                    List<ReservationSim> suivants = candidats.stream()
                            .filter(r -> !r.getId().equals(principale.getId()))
                            .filter(r -> r.getNbPassager() > 0)
                            .collect(Collectors.toList());

                    List<Allocation> optimales = choisirAllocationsOptimales(suivants, placesRestantes);

                    for (Allocation a : optimales) {
                        if (a.quantite <= 0) continue;
                        allocations.add(new ReservationSim(a.reservation, a.quantite));
                        a.reservation.remaining -= a.quantite;
                    }
                }

                // Retirer le véhicule du batch.
                vehiculesBatch.remove(choisi);

                batchTrips.add(new BatchTrip(allocations, choisi));
            }

            if (batchTrips.isEmpty()) {
                // Aucun trajet n'a pu être fait : marquer les candidats non assignables.
                for (ReservationSim c : candidats) {
                    if (c.getNbPassager() > 0) {
                        nonAssignes.add(new PlanificationNonAssigne(
                                date, c.getReservation(), c.getNbPassager(),
                                "Aucun véhicule disponible pour cette réservation"));
                        c.remaining = 0;
                    }
                }
                pending.removeIf(r -> r.getNbPassager() <= 0);
                continue;
            }

            // Calculer le départ unique du batch :
            // max(dernière heure d'arrivée de tous les candidats de la fenêtre,
            //     dernière disponibilité parmi les véhicules UTILISÉS dans le batch)
            LocalDateTime latestClientTime = candidats.stream()
                    .map(ReservationSim::getDateHeure)
                    .max(LocalDateTime::compareTo)
                    .orElse(earliest);

            LocalDateTime latestVehicleDispo = batchTrips.stream()
                    .map(bt -> vehiculeDisponible.getOrDefault(bt.vehicule.getId(), debutJournee))
                    .max(LocalDateTime::compareTo)
                    .orElse(debutJournee);

            LocalDateTime batchDepart = latestClientTime.isAfter(latestVehicleDispo)
                    ? latestClientTime : latestVehicleDispo;

            // Créer les Planning pour chaque trajet du batch.
            for (BatchTrip bt : batchTrips) {
                List<ReservationSim> routeOrdonnee = bt.allocations.stream()
                        .sorted(Comparator.comparingDouble(r ->
                                distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                        .collect(Collectors.toList());

                long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
                LocalDateTime retour = batchDepart.plusMinutes(tempsTrajetMin);

                vehiculeNbTrajets.compute(bt.vehicule.getId(), (k, v) -> v == null ? 1 : v + 1);
                vehiculeDisponible.put(bt.vehicule.getId(), retour);

                List<Planning.ResInfo> resInfos = routeOrdonnee.stream()
                        .map(r -> new Planning.ResInfo(
                                r.getId(),
                                r.getIdClient(),
                                r.getNbPassager(),
                                r.getHotel().getNom(),
                                distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                        .collect(Collectors.toList());

                List<String> routeHotels = routeOrdonnee.stream()
                        .map(r -> r.getHotel().getNom())
                        .collect(Collectors.toList());

                boolean combined = resInfos.size() > 1;
                Planning row = new Planning(resInfos, batchDepart, retour, bt.vehicule, combined, routeHotels);
                assigned.add(row);

                List<Reservation> reservationsTrajet = routeOrdonnee.stream()
                        .map(ReservationSim::getReservation)
                        .distinct()
                        .collect(Collectors.toList());

                planifications.add(new Planification(date, batchDepart, retour, bt.vehicule, combined,
                        vehiculeNbTrajets.getOrDefault(bt.vehicule.getId(), 0),
                        String.join(" \u2192 ", routeHotels), reservationsTrajet));
            }

            pending.removeIf(r -> r.getNbPassager() <= 0);
        }

        planificationService.savePlanifications(planifications);
        planificationService.saveNonAssignes(nonAssignes);

        return new SimulationResult(assigned, nonAssignes);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Choisit les meilleures allocations pour remplir les places restantes.
     * Stratégie : les plus petites réservations d'abord.
     */
    private List<Allocation> choisirAllocationsOptimales(List<ReservationSim> candidats,
                                                         int placesRestantes) {
        List<Allocation> allocations = new ArrayList<>();
        int places = placesRestantes;

        List<ReservationSim> tries = candidats.stream()
                .filter(r -> r.getNbPassager() > 0)
                .sorted(Comparator.comparingInt(ReservationSim::getNbPassager)
                        .thenComparing(ReservationSim::getDateHeure))
                .collect(Collectors.toList());

        for (ReservationSim reservation : tries) {
            if (places <= 0) break;
            int quantite = Math.min(places, reservation.getNbPassager());
            if (quantite > 0) {
                allocations.add(new Allocation(reservation, quantite));
                places -= quantite;
            }
        }

        return allocations;
    }

    /**
     * Sélectionne le meilleur véhicule parmi la liste restreinte du batch.
     * Critères : capacité minimale suffisante (ou plus grand si aucun),
     * moins de trajets, priorité Diesel, puis aléatoire.
     */
    private Vehicule choisirVehiculeDansBatch(List<Vehicule> vehiculesBatch,
                                               Map<Long, LocalDateTime> vehiculeDisponible,
                                               Map<Long, Integer> vehiculeNbTrajets,
                                               LocalDateTime maxDepart,
                                               int nbPassager,
                                               Random random) {
        List<Vehicule> candidatsDisponibles = vehiculesBatch.stream()
                .filter(v -> {
                    LocalDateTime dispo = vehiculeDisponible.getOrDefault(v.getId(), LocalDateTime.MIN);
                    return !dispo.isAfter(maxDepart);
                })
                .collect(Collectors.toList());

        if (candidatsDisponibles.isEmpty()) return null;

        // 1) Favoriser les véhicules dont la capacité est >= nbPassager
        List<Vehicule> candidats = candidatsDisponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassager)
                .collect(Collectors.toList());

        // 2) Si aucun véhicule n'est assez grand, on prend le plus grand disponible
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
     */
    private long calculerTempsTrajet(List<ReservationSim> routeOrdonnee,
                                     Hostel aeroport,
                                     int vitesseMoyenne) {
        if (routeOrdonnee.isEmpty()) return 0;

        double distanceTotale = 0;
        long idDernier = aeroport.getId();

        for (ReservationSim res : routeOrdonnee) {
            long idHotel = res.getHotel().getId();
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
