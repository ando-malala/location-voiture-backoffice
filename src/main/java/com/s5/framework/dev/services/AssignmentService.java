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
        LocalDateTime dateHeureSimulee;

        ReservationSim(Reservation reservation) {
            this.reservation = reservation;
            this.remaining = reservation.getNbPassager();
            this.dateHeureSimulee = reservation.getDateHeure();
        }

        ReservationSim(Reservation reservation, int remaining) {
            this.reservation = reservation;
            this.remaining = remaining;
            this.dateHeureSimulee = reservation.getDateHeure();
        }

        ReservationSim(ReservationSim source, int remaining) {
            this.reservation = source.reservation;
            this.remaining = remaining;
            this.dateHeureSimulee = source.dateHeureSimulee;
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
            return dateHeureSimulee;
        }

        public void setDateHeureSimulee(LocalDateTime dateHeureSimulee) {
            this.dateHeureSimulee = dateHeureSimulee;
        }

        public Hostel getHotel() {
            return reservation.getHotel();
        }

        public Reservation getReservation() {
            return reservation;
        }
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
        int vitesseMoyenne = parametreService.getVitesseMoyenne(); // km/h
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();
        Hostel aeroport = hostelRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Hôtel aéroport (id=1) non trouvé en base."));

        /* 3 — Temps d'attente */
        int tempsAttente = parametreService.getTempsAttente(); // en minutes

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
                    // Pas de véhicule dans le batch pour cette réservation, on la laisse pour le prochain batch.
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

                // Retirer le véhicule du batch (il ne peut plus servir dans cette fenêtre).
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
                        String.join(" → ", routeHotels), reservationsTrajet));
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
     * Choisit la meilleure réservation principale dans un créneau de regroupement.
     * <p>
     * On privilégie une réservation qui peut tenir dans un véhicule disponible
     * (nbPassager <= maxCapaciteDispo) et ayant le plus grand nbPassager.
     * Sinon, on retire la réservation la plus ancienne.
     */
    private ReservationSim choisirReservationPrincipale(List<ReservationSim> candidats,
                                                        int maxCapaciteDispo) {
        return candidats.stream()
                .filter(r -> r.getNbPassager() <= maxCapaciteDispo)
                .max(Comparator.comparingInt(ReservationSim::getNbPassager)
                        .thenComparing(ReservationSim::getDateHeure))
                .orElseGet(() -> candidats.stream()
                        .min(Comparator.comparing(ReservationSim::getDateHeure))
                        .orElse(candidats.get(0)));
    }

    private static class AllocationScore {
        int totalPris;
        int reservationsTerminees;
        int reservationsPartielles;
        int restesCompatibles;
        int quantiteMaxSurUneReservation;

        AllocationScore(int totalPris,
                        int reservationsTerminees,
                        int reservationsPartielles,
                        int restesCompatibles,
                        int quantiteMaxSurUneReservation) {
            this.totalPris = totalPris;
            this.reservationsTerminees = reservationsTerminees;
            this.reservationsPartielles = reservationsPartielles;
            this.restesCompatibles = restesCompatibles;
            this.quantiteMaxSurUneReservation = quantiteMaxSurUneReservation;
        }
    }

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

    private void backtrackAllocation(int index,
                                     int placesRestantes,
                                     List<ReservationSim> candidats,
                                     List<Vehicule> tousVehicules,
                                     List<Allocation> courant,
                                     List<Allocation> meilleur,
                                     AllocationScore meilleurScore) {
        if (index >= candidats.size() || placesRestantes <= 0) {
            AllocationScore score = evaluerAllocation(courant, candidats, tousVehicules);
            if (isMeilleurScore(score, meilleurScore)) {
                meilleur.clear();
                meilleur.addAll(courant.stream()
                        .filter(a -> a.quantite > 0)
                        .collect(Collectors.toList()));
                meilleurScore.totalPris = score.totalPris;
                meilleurScore.reservationsTerminees = score.reservationsTerminees;
                meilleurScore.reservationsPartielles = score.reservationsPartielles;
                meilleurScore.restesCompatibles = score.restesCompatibles;
                meilleurScore.quantiteMaxSurUneReservation = score.quantiteMaxSurUneReservation;
            }
            return;
        }

        ReservationSim res = candidats.get(index);
        int maxPrise = Math.min(placesRestantes, res.getNbPassager());

        for (int q = 0; q <= maxPrise; q++) {
            if (q > 0) {
                courant.add(new Allocation(res, q));
            }

            backtrackAllocation(index + 1,
                    placesRestantes - q,
                    candidats,
                    tousVehicules,
                    courant,
                    meilleur,
                    meilleurScore);

            if (q > 0) {
                courant.remove(courant.size() - 1);
            }
        }
    }

    private AllocationScore evaluerAllocation(List<Allocation> allocations,
                                              List<ReservationSim> candidats,
                                              List<Vehicule> tousVehicules) {
        int totalPris = allocations.stream().mapToInt(a -> a.quantite).sum();
        int quantiteMax = allocations.stream().mapToInt(a -> a.quantite).max().orElse(0);

        int reservationsTerminees = 0;
        int reservationsPartielles = 0;
        int restesCompatibles = 0;
        for (ReservationSim res : candidats) {
            int pris = allocations.stream()
                    .filter(a -> a.reservation == res)
                    .mapToInt(a -> a.quantite)
                    .sum();
            int reste = res.getNbPassager() - pris;

            if (pris > 0 && reste == 0) {
                reservationsTerminees++;
            }
            if (pris > 0 && reste > 0) {
                reservationsPartielles++;
            }

            if (reste > 0 && capaciteCompatibleExiste(reste, tousVehicules)) {
                restesCompatibles++;
            }
        }

        return new AllocationScore(totalPris, reservationsTerminees, reservationsPartielles,
                restesCompatibles, quantiteMax);
    }

    private boolean capaciteCompatibleExiste(int nbPassager, List<Vehicule> tousVehicules) {
        return tousVehicules.stream().anyMatch(v -> v.getCapacite() == nbPassager);
    }

    private boolean isMeilleurScore(AllocationScore score, AllocationScore meilleur) {
        if (score.totalPris != meilleur.totalPris) {
            return score.totalPris > meilleur.totalPris;
        }
        if (score.reservationsTerminees != meilleur.reservationsTerminees) {
            return score.reservationsTerminees > meilleur.reservationsTerminees;
        }
        if (score.reservationsPartielles != meilleur.reservationsPartielles) {
            return score.reservationsPartielles < meilleur.reservationsPartielles;
        }
        if (score.restesCompatibles != meilleur.restesCompatibles) {
            return score.restesCompatibles > meilleur.restesCompatibles;
        }
        return score.quantiteMaxSurUneReservation > meilleur.quantiteMaxSurUneReservation;
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
     * Variante de {@link #choisirVehicule} qui sélectionne parmi une liste
     * restreinte de véhicules (ceux encore disponibles dans le batch courant).
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

