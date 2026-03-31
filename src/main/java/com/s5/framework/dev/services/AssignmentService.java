package com.s5.framework.dev.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
        public final List<PlanificationNonAssigne> nonAssigned;

        public SimulationResult(List<Planning> assigned, List<PlanificationNonAssigne> nonAssigned) {
            this.assigned = assigned;
            this.nonAssigned = nonAssigned;
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
        int tempsAttente = parametreService.getTempsAttente(); // en minutes
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();
        Hostel aeroport = hostelRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Hôtel aéroport (id=1) non trouvé en base."));

        /* 3 — Réinitialiser les planifications existantes pour cette date (replanification) */
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

        Map<Long, Vehicule> vehiculesParId = tousVehicules.stream()
                .collect(Collectors.toMap(Vehicule::getId, v -> v));

        Random random = new Random();

        /* 4 — Grouper par fenêtre temps d'attente */
        List<List<Reservation>> groupes = grouperParTempsAttente(reservations, tempsAttente);
        List<Reservation> waiting = new ArrayList<>();

        for (List<Reservation> groupe : groupes) {
            if (groupe.isEmpty()) {
                continue;
            }

            LocalDateTime departGroupe = groupe.stream()
                    .map(Reservation::getDateHeure)
                    .max(LocalDateTime::compareTo)
                    .orElse(debutJournee);

            List<Reservation> groupeOrdonne = groupe.stream()
                    .sorted(Comparator.comparingDouble(r -> distanceAllerRetourKm(r, aeroport)))
                    .collect(Collectors.toList());

            List<Reservation> restants = new ArrayList<>(groupeOrdonne);

            while (!restants.isEmpty()) {
                Reservation principale = restants.stream()
                        .max(Comparator.comparingInt(Reservation::getNbPassager)
                                .thenComparing(Reservation::getDateHeure)
                                .thenComparingDouble(r -> distanceAllerRetourKm(r, aeroport)))
                        .orElse(restants.get(0));

                Vehicule choisi = choisirVehicule(tousVehicules, vehiculeDisponible, vehiculeNbTrajets,
                        departGroupe, principale.getNbPassager(), random);

                if (choisi == null) {
                    if (!capaciteDisponible(tousVehicules, principale.getNbPassager())) {
                        nonAssignes.add(new PlanificationNonAssigne(
                                date,
                                principale,
                                principale.getNbPassager(),
                                "Aucun véhicule avec capacité suffisante"));
                    } else {
                        waiting.add(principale);
                    }
                    restants.remove(principale);
                    continue;
                }

                List<Reservation> selections = remplirVehicule(principale, restants, choisi.getCapacite());
                restants.removeAll(selections);

                List<Reservation> routeOrdonnee = ordonnerParProcheEnProche(selections, aeroport);
                LocalDateTime depart = departGroupe;
                long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
                LocalDateTime retour = depart.plusMinutes(tempsTrajetMin);

                int nbTrajet = vehiculeNbTrajets.compute(choisi.getId(), (k, v) -> v == null ? 1 : v + 1);
                vehiculeDisponible.put(choisi.getId(), retour);

                enregistrerTrajet(date, routeOrdonnee, depart, retour, choisi, nbTrajet,
                        aeroport, assigned, planifications);
            }
        }

        /* 5 — Sprint 8 : réactivité au retour des véhicules (fenêtre 30 min) */
        List<Reservation> sprintPending = waiting.stream()
                .sorted(Comparator.comparing(Reservation::getDateHeure))
                .collect(Collectors.toList());

        while (!sprintPending.isEmpty()) {
            Map.Entry<Long, LocalDateTime> prochainRetour = vehiculeDisponible.entrySet()
                    .stream()
                    .min(Map.Entry.comparingByValue())
                    .orElse(null);

            if (prochainRetour == null) {
                break;
            }

            Vehicule vehicule = vehiculesParId.get(prochainRetour.getKey());
            if (vehicule == null) {
                break;
            }

            boolean planifie = planifierSurRetourVehicule(date, vehicule, prochainRetour.getValue(),
                    sprintPending, aeroport, vitesseMoyenne, vehiculeDisponible, vehiculeNbTrajets,
                    assigned, planifications);

            if (!planifie) {
                break;
            }
        }

        for (Reservation res : sprintPending) {
            nonAssignes.add(new PlanificationNonAssigne(
                    date,
                    res,
                    res.getNbPassager(),
                    "Aucun véhicule disponible"));
        }

        planificationService.savePlanifications(planifications);
        planificationService.saveNonAssignes(nonAssignes);

        return new SimulationResult(assigned, nonAssignes);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private List<List<Reservation>> grouperParTempsAttente(List<Reservation> reservations,
                                                          int tempsAttenteMinutes) {
        List<List<Reservation>> groupes = new ArrayList<>();

        int index = 0;
        while (index < reservations.size()) {
            Reservation first = reservations.get(index);
            LocalDateTime start = first.getDateHeure();
            LocalDateTime end = start.plusMinutes(tempsAttenteMinutes);

            List<Reservation> group = new ArrayList<>();
            while (index < reservations.size()) {
                Reservation current = reservations.get(index);
                if (current.getDateHeure().isAfter(end)) {
                    break;
                }
                group.add(current);
                index++;
            }
            groupes.add(group);
        }

        return groupes;
    }

    private double distanceAllerRetourKm(Reservation reservation, Hostel aeroport) {
        return distanceService.getDistanceKm(aeroport.getId(), reservation.getHotel().getId()) * 2;
    }

    private boolean capaciteDisponible(List<Vehicule> vehicules, int nbPassager) {
        return vehicules.stream().anyMatch(v -> v.getCapacite() >= nbPassager);
    }

    private List<Reservation> remplirVehicule(Reservation principale,
                                              List<Reservation> candidats,
                                              int capacite) {
        List<Reservation> selections = new ArrayList<>();
        selections.add(principale);

        int placesRestantes = capacite - principale.getNbPassager();
        List<Reservation> autres = candidats.stream()
                .filter(r -> !r.equals(principale))
                .collect(Collectors.toList());

        while (placesRestantes > 0) {
            Reservation suivant = null;
            for (Reservation r : autres) {
                if (r.getNbPassager() > placesRestantes) {
                    continue;
                }
                if (suivant == null
                        || r.getNbPassager() > suivant.getNbPassager()
                        || (r.getNbPassager().equals(suivant.getNbPassager())
                        && r.getDateHeure().isBefore(suivant.getDateHeure()))) {
                    suivant = r;
                }
            }

            if (suivant == null) {
                break;
            }

            selections.add(suivant);
            autres.remove(suivant);
            placesRestantes -= suivant.getNbPassager();
        }

        return selections;
    }

    private List<Reservation> ordonnerParProcheEnProche(List<Reservation> reservations, Hostel aeroport) {
        if (reservations.isEmpty()) {
            return reservations;
        }

        Map<Long, List<Reservation>> parHotel = reservations.stream()
                .collect(Collectors.groupingBy(r -> r.getHotel().getId()));
        Set<Long> hotelsRestants = new HashSet<>(parHotel.keySet());

        List<Reservation> ordered = new ArrayList<>();
        long courant = aeroport.getId();

        while (!hotelsRestants.isEmpty()) {
            Long prochainHotel = null;
            double meilleureDistance = Double.MAX_VALUE;

            for (Long hotelId : hotelsRestants) {
                double distance = distanceService.getDistanceKm(courant, hotelId);
                if (distance < meilleureDistance) {
                    meilleureDistance = distance;
                    prochainHotel = hotelId;
                }
            }

            if (prochainHotel == null) {
                break;
            }

            List<Reservation> reservationsHotel = parHotel.getOrDefault(prochainHotel, List.of());
            reservationsHotel.stream()
                    .sorted(Comparator.comparing(Reservation::getDateHeure))
                    .forEach(ordered::add);

            hotelsRestants.remove(prochainHotel);
            courant = prochainHotel;
        }

        return ordered;
    }

    private void enregistrerTrajet(LocalDate date,
                                   List<Reservation> routeOrdonnee,
                                   LocalDateTime depart,
                                   LocalDateTime retour,
                                   Vehicule vehicule,
                                   int nbTrajet,
                                   Hostel aeroport,
                                   List<Planning> assigned,
                                   List<Planification> planifications) {
        if (routeOrdonnee.isEmpty()) {
            return;
        }

        List<Planning.ResInfo> resInfos = new ArrayList<>();
        List<String> routeHotels = new ArrayList<>();

        long idPrecedent = aeroport.getId();
        String nomPrecedent = "Aeroport";

        for (Reservation res : routeOrdonnee) {
            long idHotel = res.getHotel().getId();
            double distanceSegment = 0;
            if (idHotel != idPrecedent) {
                distanceSegment = distanceService.getDistanceKm(idPrecedent, idHotel);
            }
            double distanceAeroportHotel = distanceService.getDistanceKm(aeroport.getId(), idHotel);

            resInfos.add(new Planning.ResInfo(
                    res.getId(),
                    res.getIdClient(),
                    res.getNbPassager(),
                    res.getHotel().getNom(),
                    distanceSegment,
                    distanceAeroportHotel,
                    nomPrecedent));

            routeHotels.add(res.getHotel().getNom());
            idPrecedent = idHotel;
            nomPrecedent = res.getHotel().getNom();
        }

        boolean combined = resInfos.size() > 1;
        assigned.add(new Planning(resInfos, depart, retour, vehicule, combined, routeHotels));
        planifications.add(new Planification(date, depart, retour, vehicule, combined, nbTrajet,
                String.join(" → ", routeHotels), routeOrdonnee));
    }

    private boolean planifierSurRetourVehicule(LocalDate date,
                                               Vehicule vehicule,
                                               LocalDateTime retour,
                                               List<Reservation> pending,
                                               Hostel aeroport,
                                               int vitesseMoyenne,
                                               Map<Long, LocalDateTime> vehiculeDisponible,
                                               Map<Long, Integer> vehiculeNbTrajets,
                                               List<Planning> assigned,
                                               List<Planification> planifications) {
        if (pending.isEmpty()) {
            return false;
        }

        LocalDateTime windowEnd = retour.plusMinutes(30);
        List<Reservation> candidats = pending.stream()
                .filter(r -> !r.getDateHeure().isAfter(windowEnd))
                .collect(Collectors.toList());

        if (candidats.isEmpty()) {
            return false;
        }

        Reservation principale = candidats.stream()
                .filter(r -> r.getNbPassager() <= vehicule.getCapacite())
                .max(Comparator.comparingInt(Reservation::getNbPassager)
                        .thenComparing(Reservation::getDateHeure))
                .orElse(null);

        if (principale == null) {
            return false;
        }

        List<Reservation> selections = remplirVehicule(principale, candidats, vehicule.getCapacite());
        pending.removeAll(selections);

        List<Reservation> routeOrdonnee = ordonnerParProcheEnProche(selections, aeroport);
        int totalPassagers = selections.stream().mapToInt(Reservation::getNbPassager).sum();
        boolean plein = totalPassagers >= vehicule.getCapacite();

        LocalDateTime maxArrivee = selections.stream()
                .map(Reservation::getDateHeure)
                .max(LocalDateTime::compareTo)
                .orElse(retour);

        LocalDateTime depart = plein ? maxArrivee : windowEnd;
        if (depart.isBefore(retour)) {
            depart = retour;
        }

        long tempsTrajetMin = calculerTempsTrajet(routeOrdonnee, aeroport, vitesseMoyenne);
        LocalDateTime retourTrajet = depart.plusMinutes(tempsTrajetMin);

        int nbTrajet = vehiculeNbTrajets.compute(vehicule.getId(), (k, v) -> v == null ? 1 : v + 1);
        vehiculeDisponible.put(vehicule.getId(), retourTrajet);

        enregistrerTrajet(date, routeOrdonnee, depart, retourTrajet, vehicule, nbTrajet,
                aeroport, assigned, planifications);

        return true;
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

        // 1) Garder uniquement les véhicules pouvant contenir nbPassager
        List<Vehicule> candidats = candidatsDisponibles.stream()
            .filter(v -> v.getCapacite() >= nbPassager)
            .collect(Collectors.toList());

        if (candidats.isEmpty()) return null;

        // 2) Capacité minimale suffisante (best-fit)
        int minCap = candidats.stream().mapToInt(Vehicule::getCapacite).min().getAsInt();
        List<Vehicule> capaciteMin = candidats.stream()
            .filter(v -> v.getCapacite() == minCap)
            .collect(Collectors.toList());

        // 3) Moins de trajets déjà effectués
        int minTrajets = capaciteMin.stream()
            .mapToInt(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0))
            .min()
            .orElse(0);
        List<Vehicule> meilleurs = capaciteMin.stream()
            .filter(v -> vehiculeNbTrajets.getOrDefault(v.getId(), 0) == minTrajets)
            .collect(Collectors.toList());

        if (meilleurs.size() == 1) return meilleurs.get(0);

        // 4) Préférence Diesel, sinon aléatoire
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

