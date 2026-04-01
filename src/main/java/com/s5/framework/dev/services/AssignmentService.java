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
 *   <li>Assignation individuelle en priorité :
 *       <ul>
 *         <li>Pour chaque réservation (la plus grande d'abord), on cherche le véhicule
 *             disponible dont la capacité est la <strong>plus proche</strong> (≥) du
 *             nombre de passagers de cette réservation seule.</li>
 *         <li>Une fois ce véhicule identifié, on remplit ses places restantes avec les
 *             réservations suivantes du même créneau (les plus petites d'abord).</li>
 *         <li>Si aucun véhicule n'est disponible pour une réservation, elle est marquée
 *             non assignée.</li>
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

    private static class Allocation {
        final ReservationSim reservation;
        final int quantite;

        Allocation(ReservationSim reservation, int quantite) {
            this.reservation = reservation;
            this.quantite = quantite;
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

<<<<<<< HEAD
<<<<<<< HEAD
        /* 3 — Regrouper par créneau (dateHeure identique) en conservant l'ordre */
        Map<LocalDateTime, List<Reservation>> groupes = reservations.stream()
                .collect(Collectors.groupingBy(
                        Reservation::getDateHeure,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Planning> assigned = new ArrayList<>();
        List<Reservation> unassigned = new ArrayList<>();
        Set<Long> vehiculesUtilises = new HashSet<>();
        Random random = new Random();

        /* 4 — Traiter chaque créneau */
        for (Map.Entry<LocalDateTime, List<Reservation>> entry : groupes.entrySet()) {
            LocalDateTime creneau = entry.getKey();
=======
=======
>>>>>>> parent of 3c9df05 (Fix vrai)
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
            LocalDateTime disponibiliteInitiale = v.getHeureDispo() != null
                    ? date.atTime(v.getHeureDispo())
                    : debutJournee;
            vehiculeDisponible.put(v.getId(), disponibiliteInitiale);
            vehiculeNbTrajets.put(v.getId(), 0);
        }

        Random random = new Random();

        /* 5 — Traiter les réservations en prenant en compte le temps d'attente. */
        List<ReservationSim> pending = new ArrayList<>(reservations);
        ReservationSim reservationEnCours = null;
<<<<<<< HEAD
>>>>>>> parent of 3c9df05 (Fix vrai)
=======
>>>>>>> parent of 3c9df05 (Fix vrai)

            /* Trier par nbPassager décroissant : priorité au plus grand groupe */
            List<Reservation> groupe = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(Reservation::getNbPassager).reversed())
                    .collect(Collectors.toList());

            Set<Long> dejaPrisEnCharge = new HashSet<>();

<<<<<<< HEAD
<<<<<<< HEAD
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
                TrajetAssigne resultat = tenteCombine(
                        candidatesTrajet, tousVehicules, vehiculesUtilises, random);

                if (resultat == null) {
                    // Aucun véhicule même pour la seule réservation principale
                    unassigned.add(principale);
                    dejaPrisEnCharge.add(principale.getId());
                    continue;
                }

                List<Reservation> trajet = resultat.trajet;
                Vehicule choisi = resultat.vehicule;
                vehiculesUtilises.add(choisi.getId());

                /* Construire la route : hotels triés par distance croissante depuis l'aéroport */
                List<Reservation> routeOrdonnee = trajet.stream()
                        .sorted(Comparator.comparingDouble(r ->
                                distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                        .collect(Collectors.toList());
=======
            // Si un split est en cours, finir celui-ci avant de passer à un autre client.
            ReservationSim principale;
            if (reservationEnCours != null && pending.contains(reservationEnCours)) {
                principale = reservationEnCours;
            } else {
                principale = candidats.stream()
                        .max(Comparator.comparingInt(ReservationSim::getNbPassager)
                                .thenComparing(ReservationSim::getDateHeure))
                        .orElse(candidats.get(0));
                reservationEnCours = null;
            }

            List<Vehicule> disponibles = tousVehicules.stream()
                    .filter(v -> !vehiculeDisponible.getOrDefault(v.getId(), debutJournee).isAfter(maxDepart))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                LocalDateTime prochaineDisponibilite = vehiculeDisponible.values().stream()
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

                if (prochaineDisponibilite != null) {
                    LocalDateTime prochaineReservationFuture = pending.stream()
                            .filter(r -> r != principale)
                            .map(ReservationSim::getDateHeure)
                            .filter(h -> h.isAfter(principale.getDateHeure()))
                            .min(LocalDateTime::compareTo)
                            .orElse(null);

                    LocalDateTime nouveauCreneau = prochaineDisponibilite;
                    if (prochaineReservationFuture != null && prochaineReservationFuture.isAfter(nouveauCreneau)) {
                        nouveauCreneau = prochaineReservationFuture;
                    }

                    principale.setDateHeureSimulee(nouveauCreneau);
                    continue;
                }

                nonAssignes.add(new PlanificationNonAssigne(
                    date,
                    principale.getReservation(),
                    principale.getNbPassager(),
                    "Aucun véhicule disponible"));
                pending.remove(principale);
                continue;
            }

            Vehicule choisi = choisirVehicule(tousVehicules, vehiculeDisponible, vehiculeNbTrajets,
                    maxDepart, principale.getNbPassager(), random);

            if (choisi == null) {
                nonAssignes.add(new PlanificationNonAssigne(
                        date,
                        principale.getReservation(),
                        principale.getNbPassager(),
                        "Aucun véhicule disponible pour cette réservation"));
                pending.remove(principale);
                continue;
            }

            int pris = Math.min(principale.getNbPassager(), choisi.getCapacite());
                int placesRestantes = choisi.getCapacite() - pris;

                List<ReservationSim> allocations = new ArrayList<>();
                allocations.add(new ReservationSim(principale, pris));

                principale.remaining -= pris;

                ReservationSim prochaineReservationEnCours = principale.getNbPassager() > 0 ? principale : null;

                // Remplir les places restantes avec une allocation optimale globale.
                if (placesRestantes > 0) {
                    List<ReservationSim> suivants = candidats.stream()
                    .filter(r -> !r.getId().equals(principale.getId()))
                    .filter(r -> r.getNbPassager() > 0)
                    .collect(Collectors.toList());

                    List<Allocation> optimales = choisirAllocationsOptimales(
                            suivants, placesRestantes);

                    for (Allocation a : optimales) {
                        if (a.quantite <= 0) continue;
                        allocations.add(new ReservationSim(a.reservation, a.quantite));
                        a.reservation.remaining -= a.quantite;

                }
                }

                List<ReservationSim> routeOrdonnee = allocations.stream()
                    .sorted(Comparator.comparingDouble(r ->
                        distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                    .collect(Collectors.toList());

                LocalDateTime heureDernierClient = routeOrdonnee.stream()
                    .map(ReservationSim::getDateHeure)
                    .max(LocalDateTime::compareTo)
                    .orElse(principale.getDateHeure());

                LocalDateTime dispoVehicule = vehiculeDisponible
                    .getOrDefault(choisi.getId(), debutJournee);

                LocalDateTime depart = heureDernierClient.isAfter(dispoVehicule)
                    ? heureDernierClient
                    : dispoVehicule;
>>>>>>> parent of 3c9df05 (Fix vrai)

                /* Calculer le temps du trajet multi-stops */
                long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
<<<<<<< HEAD
                LocalDateTime retour = creneau.plusMinutes(tempsTrajetMin);

                /* Noms des hôtels dans l'ordre de visite (pour affichage) */
                List<String> routeHotels = routeOrdonnee.stream()
                        .map(r -> r.getHotel().getNom())
                        .collect(Collectors.toList());
                boolean combined = trajet.size() > 1;
=======
                LocalDateTime retour = depart.plusMinutes(tempsTrajetMin);

                vehiculeNbTrajets.compute(choisi.getId(), (k, v) -> v == null ? 1 : v + 1);
                vehiculeDisponible.put(choisi.getId(), retour);
>>>>>>> parent of 3c9df05 (Fix vrai)

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
                Planning row = new Planning(resInfos, creneau, retour, choisi, combined, routeHotels);
                assigned.add(row);

                /* Marquer toutes les réservations du trajet comme traitées */
                for (Reservation res : trajet) {
                    dejaPrisEnCharge.add(res.getId());
                }
                pending.removeIf(r -> r.getNbPassager() <= 0);
                continue;
            }

            // Calculer le départ unique du batch :
            // max(dernière heure d'arrivée de tous les candidats de la fenêtre,
            //     dernière disponibilité parmi les véhicules UTILISÉS dans le batch)
            LocalDateTime latestClientTime = candidats.stream()
=======
            // Si un split est en cours, finir celui-ci avant de passer à un autre client.
            ReservationSim principale;
            if (reservationEnCours != null && pending.contains(reservationEnCours)) {
                principale = reservationEnCours;
            } else {
                principale = candidats.stream()
                        .max(Comparator.comparingInt(ReservationSim::getNbPassager)
                                .thenComparing(ReservationSim::getDateHeure))
                        .orElse(candidats.get(0));
                reservationEnCours = null;
            }

            List<Vehicule> disponibles = tousVehicules.stream()
                    .filter(v -> !vehiculeDisponible.getOrDefault(v.getId(), debutJournee).isAfter(maxDepart))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                LocalDateTime prochaineDisponibilite = vehiculeDisponible.values().stream()
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

                if (prochaineDisponibilite != null) {
                    LocalDateTime prochaineReservationFuture = pending.stream()
                            .filter(r -> r != principale)
                            .map(ReservationSim::getDateHeure)
                            .filter(h -> h.isAfter(principale.getDateHeure()))
                            .min(LocalDateTime::compareTo)
                            .orElse(null);

                    LocalDateTime nouveauCreneau = prochaineDisponibilite;
                    if (prochaineReservationFuture != null && prochaineReservationFuture.isAfter(nouveauCreneau)) {
                        nouveauCreneau = prochaineReservationFuture;
                    }

                    principale.setDateHeureSimulee(nouveauCreneau);
                    continue;
                }

                nonAssignes.add(new PlanificationNonAssigne(
                    date,
                    principale.getReservation(),
                    principale.getNbPassager(),
                    "Aucun véhicule disponible"));
                pending.remove(principale);
                continue;
            }

            Vehicule choisi = choisirVehicule(tousVehicules, vehiculeDisponible, vehiculeNbTrajets,
                    maxDepart, principale.getNbPassager(), random);

            if (choisi == null) {
                nonAssignes.add(new PlanificationNonAssigne(
                        date,
                        principale.getReservation(),
                        principale.getNbPassager(),
                        "Aucun véhicule disponible pour cette réservation"));
                pending.remove(principale);
                continue;
            }

            int pris = Math.min(principale.getNbPassager(), choisi.getCapacite());
                int placesRestantes = choisi.getCapacite() - pris;

                List<ReservationSim> allocations = new ArrayList<>();
                allocations.add(new ReservationSim(principale, pris));

                principale.remaining -= pris;

                ReservationSim prochaineReservationEnCours = principale.getNbPassager() > 0 ? principale : null;

                // Remplir les places restantes avec une allocation optimale globale.
                if (placesRestantes > 0) {
                    List<ReservationSim> suivants = candidats.stream()
                    .filter(r -> !r.getId().equals(principale.getId()))
                    .filter(r -> r.getNbPassager() > 0)
                    .collect(Collectors.toList());

                    List<Allocation> optimales = choisirAllocationsOptimales(
                            suivants, placesRestantes);

                    for (Allocation a : optimales) {
                        if (a.quantite <= 0) continue;
                        allocations.add(new ReservationSim(a.reservation, a.quantite));
                        a.reservation.remaining -= a.quantite;

                }
                }

                List<ReservationSim> routeOrdonnee = allocations.stream()
                    .sorted(Comparator.comparingDouble(r ->
                        distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                    .collect(Collectors.toList());

                LocalDateTime heureDernierClient = routeOrdonnee.stream()
>>>>>>> parent of 3c9df05 (Fix vrai)
                    .map(ReservationSim::getDateHeure)
                    .max(LocalDateTime::compareTo)
                    .orElse(principale.getDateHeure());

                LocalDateTime dispoVehicule = vehiculeDisponible
                    .getOrDefault(choisi.getId(), debutJournee);

                LocalDateTime depart = heureDernierClient.isAfter(dispoVehicule)
                    ? heureDernierClient
                    : dispoVehicule;

                long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
                LocalDateTime retour = depart.plusMinutes(tempsTrajetMin);

                vehiculeNbTrajets.compute(choisi.getId(), (k, v) -> v == null ? 1 : v + 1);
                vehiculeDisponible.put(choisi.getId(), retour);

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
                Planning row = new Planning(resInfos, depart, retour, choisi, combined, routeHotels);
                assigned.add(row);

                List<Reservation> reservationsTrajet = routeOrdonnee.stream()
                    .map(ReservationSim::getReservation)
                    .distinct()
                    .collect(Collectors.toList());

                planifications.add(new Planification(date, depart, retour, choisi, combined,
                    String.join(" → ", routeHotels), reservationsTrajet));

                pending.removeIf(r -> r.getNbPassager() <= 0);
                reservationEnCours = (prochaineReservationEnCours != null && pending.contains(prochaineReservationEnCours))
                    ? prochaineReservationEnCours
                    : null;

                if (reservationEnCours == null) {
                pending.sort(Comparator.comparingInt(ReservationSim::getNbPassager).reversed()
                    .thenComparing(ReservationSim::getDateHeure));
                }
        }

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
                                       Set<Long> vehiculesUtilises,
                                       Random random) {
        Reservation principale = candidates.get(0);

        /* 1 — Chercher le véhicule le mieux adapté à la réservation principale seule */
        Vehicule vehiculePrincipal = choisirVehicule(
                tousVehicules, vehiculesUtilises, principale.getNbPassager(), random);

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

<<<<<<< HEAD
        int minCap = candidats.stream().mapToInt(Vehicule::getCapacite).min().getAsInt();
        List<Vehicule> plusPetits = candidats.stream()
=======
        int minTrajets = candidats.stream()
                .mapToInt(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0))
                .min()
                .orElse(0);
        List<Vehicule> meilleurs = candidats.stream()
                .filter(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0) == minTrajets)
                .collect(Collectors.toList());

        int minCap = meilleurs.stream().mapToInt(Vehicule::getCapacite).min().getAsInt();
        List<Vehicule> plusPetits = meilleurs.stream()
>>>>>>> parent of 3c9df05 (Fix vrai)
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

