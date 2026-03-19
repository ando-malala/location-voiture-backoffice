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
 *         <li>Si une réservation dépasse la taille du plus grand véhicule, elle peut être scindée :
 *             on utilise d'abord toutes les places disponibles, et le reste est traité avant
 *             toute autre réservation.</li>
 *         <li>Si aucun véhicule n'est disponible, la réservation (ou le reste) est marquée
 *             non assignée.</li>
 *       </ul>
 *   </li>
 *   <li>Sélection du véhicule : priorité aux véhicules ayant le moins de trajets,
 *       puis capacité minimale suffisante → priorité Diesel → aléatoire.</li>
 *   <li>Un véhicule peut être réutilisé après son heure de retour.</li>
 *   <li>Pour un trajet combiné, la voiture visite les hôtels <strong>du plus proche
 *       au plus éloigné</strong> de l'aéroport ; le temps de retour est calculé sur
 *       l'intégralité du trajet multi-stops.</li>
 * </ol>
 *
 * Les planifications sont persistées en base. Lors d'une replanification, celles de
 * la date donnée sont supprimées puis recalculées.
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

    /** Réservation segment (peut être partielle si la réservation est scindée). */
    private static class ReservationSegment {
        final Reservation reservation;
        int remaining;

        ReservationSegment(Reservation reservation) {
            this.reservation = reservation;
            this.remaining = reservation.getNbPassager();
        }

        ReservationSegment(Reservation reservation, int remaining) {
            this.reservation = reservation;
            this.remaining = remaining;
        }

        public Long getId() { return reservation.getId(); }
        public String getIdClient() { return reservation.getIdClient(); }
        public int getNbPassager() { return remaining; }
        public LocalDateTime getDateHeure() { return reservation.getDateHeure(); }
        public Hostel getHotel() { return reservation.getHotel(); }
        public Reservation getReservation() { return reservation; }
    }

    /** Résultat d'un trajet : réservations assignées + véhicule + reste non assigné (optionnel). */
    private static class TrajetAssigne {
        final List<ReservationSegment> trajet;
        final Vehicule vehicule;
        final ReservationSegment partialRemaining;

        TrajetAssigne(List<ReservationSegment> trajet, Vehicule vehicule, ReservationSegment partialRemaining) {
            this.trajet = trajet;
            this.vehicule = vehicule;
            this.partialRemaining = partialRemaining;
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
        List<ReservationSegment> reservations = reservationService.findByDate(date)
                .stream()
                .sorted(Comparator.comparing(Reservation::getDateHeure))
                .map(ReservationSegment::new)
                .collect(Collectors.toList());

        /* 2 — Paramètres */
        int vitesseMoyenne = parametreService.getVitesseMoyenne(); // km/h
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();
        Hostel aeroport = hostelRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Hôtel aéroport (id=1) non trouvé en base."));

        int tempsAttente = parametreService.getTempsAttente(); // minutes

        /* 3 — Réinitialiser les planifications existantes pour cette date (replanification) */
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

        /* 4 — Traiter les réservations en prenant en compte le temps d'attente. */
        List<ReservationSegment> pending = new ArrayList<>(reservations);

        while (!pending.isEmpty()) {
            ReservationSegment principale = pending.get(0);
            LocalDateTime maxDepart = principale.getDateHeure().plusMinutes(tempsAttente);

            /* Candidats pour ce créneau (temps d'attente) */
            List<ReservationSegment> candidats = pending.stream()
                    .filter(r -> !r.getDateHeure().isAfter(maxDepart))
                    .collect(Collectors.toList());

            /* Ordre de traitement : principale puis les plus grands groupes */
            List<ReservationSegment> candidatesTrajet = new ArrayList<>();
            candidatesTrajet.add(principale);
            candidatesTrajet.addAll(candidats.stream()
                    .filter(r -> !r.getId().equals(principale.getId()))
                    .sorted(Comparator.comparingInt(ReservationSegment::getNbPassager).reversed()
                            .thenComparing(ReservationSegment::getDateHeure))
                    .collect(Collectors.toList()));

            /* Véhicules disponibles au départ */
            List<Vehicule> disponibles = tousVehicules.stream()
                    .filter(v -> !vehiculeDisponible.getOrDefault(v.getId(), debutJournee).isAfter(maxDepart))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                nonAssignes.add(new PlanificationNonAssigne(
                        date,
                        principale.getReservation(),
                        principale.getNbPassager(),
                        "Aucun véhicule disponible à l'heure de départ"));
                pending.remove(0);
                continue;
            }

            int maxCapaciteDispo = disponibles.stream().mapToInt(Vehicule::getCapacite).max().orElse(0);

            /* 4.1 — Scinder la réservation si elle dépasse la capacité du plus grand véhicule */
            if (principale.getNbPassager() > maxCapaciteDispo) {
                int reste = principale.getNbPassager();

                while (reste > 0) {
                    List<Vehicule> disponiblesSplit = tousVehicules.stream()
                            .filter(v -> !vehiculeDisponible.getOrDefault(v.getId(), debutJournee).isAfter(maxDepart))
                            .collect(Collectors.toList());

                    Vehicule choisi = choisirVehiculePourScission(disponiblesSplit, vehiculeNbTrajets, random);
                    if (choisi == null) {
                        nonAssignes.add(new PlanificationNonAssigne(
                                date,
                                principale.getReservation(),
                                reste,
                                "Reste de la réservation non assigné (" + reste + " passagers)"));
                        break;
                    }

                    int pris = Math.min(reste, choisi.getCapacite());
                    reste -= pris;

                    LocalDateTime depart = maxDepart;
                    double distance = distanceService.getDistanceKm(aeroport.getId(), principale.getHotel().getId());
                    long tempsTrajetMin = Math.round((distance * 2 / vitesseMoyenne) * 60);
                    LocalDateTime retour = depart.plusMinutes(tempsTrajetMin);

                    vehiculeNbTrajets.compute(choisi.getId(), (k, v) -> v == null ? 1 : v + 1);
                    vehiculeDisponible.put(choisi.getId(), retour);

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

                if (reste > 0) {
                    principale.remaining = reste;
                } else {
                    pending.remove(0);
                }
                continue;
            }

            /* 4.2 — Réservation tient dans un véhicule : essayer de combiner */
            TrajetAssigne resultat = tenteCombine(
                    candidatesTrajet, tousVehicules,
                    vehiculeDisponible, vehiculeNbTrajets,
                    maxDepart, random);

            if (resultat == null) {
                nonAssignes.add(new PlanificationNonAssigne(
                        date,
                        principale.getReservation(),
                        principale.getNbPassager(),
                        "Aucun véhicule avec capacité ≥ " + principale.getNbPassager() + " pl. disponible"));
                pending.remove(0);
                continue;
            }

            List<ReservationSegment> trajet = resultat.trajet;
            Vehicule choisi = resultat.vehicule;

            LocalDateTime depart = trajet.stream()
                    .map(ReservationSegment::getDateHeure)
                    .max(LocalDateTime::compareTo)
                    .orElse(principale.getDateHeure());

            List<ReservationSegment> routeOrdonnee = trajet.stream()
                    .sorted(Comparator.comparingDouble(r ->
                            distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                    .collect(Collectors.toList());

            long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
            LocalDateTime retour = depart.plusMinutes(tempsTrajetMin);

            vehiculeNbTrajets.compute(choisi.getId(), (k, v) -> v == null ? 1 : v + 1);
            vehiculeDisponible.put(choisi.getId(), retour);

            List<String> routeHotels = routeOrdonnee.stream()
                    .map(r -> r.getHotel().getNom())
                    .collect(Collectors.toList());
            boolean combined = trajet.size() > 1;

            List<Planning.ResInfo> resInfos = routeOrdonnee.stream()
                    .map(r -> new Planning.ResInfo(
                            r.getId(),
                            r.getIdClient(),
                            r.getNbPassager(),
                            r.getHotel().getNom(),
                            distanceService.getDistanceKm(aeroport.getId(), r.getHotel().getId())))
                    .collect(Collectors.toList());

            Planning row = new Planning(resInfos, depart, retour, choisi, combined, routeHotels);
            assigned.add(row);

            planifications.add(new Planification(date, depart, retour, choisi, combined,
                    String.join(" → ", routeHotels),
                    trajet.stream().map(ReservationSegment::getReservation).collect(Collectors.toList())));

            pending.removeIf(r -> r.getNbPassager() == 0);
            if (resultat.partialRemaining != null) {
                pending.remove(resultat.partialRemaining);
                pending.add(0, resultat.partialRemaining);
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
    private TrajetAssigne tenteCombine(List<ReservationSegment> candidates,
                                       List<Vehicule> tousVehicules,
                                       Map<Long, LocalDateTime> vehiculeDisponible,
                                       Map<Long, Integer> vehiculeNbTrajets,
                                       LocalDateTime depart,
                                       Random random) {
        ReservationSegment principale = candidates.get(0);

        Vehicule vehiculePrincipal = choisirVehicule(
                tousVehicules, vehiculeDisponible, vehiculeNbTrajets, depart,
                principale.getNbPassager(), random);

        if (vehiculePrincipal == null) {
            return null;
        }

        List<ReservationSegment> trajet = new ArrayList<>();
        trajet.add(principale);
        int placesRestantes = vehiculePrincipal.getCapacite() - principale.getNbPassager();
        principale.remaining = 0;

        List<ReservationSegment> suivantes = candidates.subList(1, candidates.size())
                .stream()
                .filter(r -> r.getNbPassager() > 0)
                .sorted(Comparator.comparingInt(ReservationSegment::getNbPassager).reversed())
                .collect(Collectors.toList());

        ReservationSegment partialRemaining = null;

        for (ReservationSegment suivante : suivantes) {
            if (placesRestantes <= 0) break;

            int demande = suivante.getNbPassager();
            if (demande <= placesRestantes) {
                trajet.add(suivante);
                placesRestantes -= demande;
                suivante.remaining = 0;
            } else {
                ReservationSegment part = new ReservationSegment(suivante.getReservation(), placesRestantes);
                trajet.add(part);
                suivante.remaining = demande - placesRestantes;
                partialRemaining = suivante;
                break;
            }
        }

        return new TrajetAssigne(trajet, vehiculePrincipal, partialRemaining);
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
        List<Vehicule> candidatsDisponibles = tousVehicules.stream()
                .filter(v -> {
                    LocalDateTime dispo = vehiculeDisponible.getOrDefault(v.getId(), LocalDateTime.MIN);
                    return !dispo.isAfter(depart);
                })
                .collect(Collectors.toList());

        if (candidatsDisponibles.isEmpty()) return null;

        List<Vehicule> candidats = candidatsDisponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassager)
                .collect(Collectors.toList());

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
     * Choisit un véhicule pour scinder une réservation trop grande (on choisit
     * d'abord la capacité maximale disponible, puis les mêmes critères de priorité).
     */
    private Vehicule choisirVehiculePourScission(List<Vehicule> disponibles,
                                                 Map<Long, Integer> vehiculeNbTrajets,
                                                 Random random) {
        if (disponibles.isEmpty()) return null;

        int maxCap = disponibles.stream().mapToInt(Vehicule::getCapacite).max().orElse(0);

        List<Vehicule> candidats = disponibles.stream()
                .filter(v -> v.getCapacite() == maxCap)
                .collect(Collectors.toList());

        int minTrajets = candidats.stream()
                .mapToInt(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0))
                .min()
                .orElse(0);

        List<Vehicule> meilleurs = candidats.stream()
                .filter(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0) == minTrajets)
                .collect(Collectors.toList());

        if (meilleurs.size() == 1) return meilleurs.get(0);

        List<Vehicule> diesels = meilleurs.stream()
                .filter(v -> "Diesel".equalsIgnoreCase(v.getTypeCarburant().getLibelle()))
                .collect(Collectors.toList());

        if (!diesels.isEmpty()) return diesels.get(random.nextInt(diesels.size()));
        return meilleurs.get(random.nextInt(meilleurs.size()));
    }

    /**
     * Calcule le temps total (en minutes) d'un trajet multi-stops.
     * Route : Aéroport → hotel_1 → hotel_2 → … → hotel_N → Aéroport
     * ({@code routeOrdonnee} est déjà triée du plus proche au plus éloigné).
     */
    private long calculerTempsTrajet(List<ReservationSegment> routeOrdonnee,
                                     Hostel aeroport,
                                     int vitesseMoyenne) {
        if (routeOrdonnee.isEmpty()) return 0;

        double distanceTotale = 0;
        long idDernier = aeroport.getId();

        for (ReservationSegment res : routeOrdonnee) {
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


