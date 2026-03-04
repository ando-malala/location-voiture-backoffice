package com.s5.framework.dev.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
 * Logique d'assignation (par réservation, triées par heure croissante) :
 *  1. Ne considérer que les véhicules non encore utilisés dans la simulation.
 *  2. Parmi eux, garder ceux dont capacite >= nbPassager.
 *  3. Choisir celui avec la plus petite capacité (closest fitting).
 *  4. À égalité de capacité : priorité au Diesel.
 *  5. À égalité de capacité ET typeCarburant : choix aléatoire.
 *  6. Aucun véhicule trouvé → réservation non assignée.
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

    /**
     * Résultat de la simulation pour une date donnée.
     */
    public static class SimulationResult {
        public final List<Planning> assigned;
        public final List<Long> unassignedIds;

        public SimulationResult(List<Planning> assigned, List<Long> unassignedIds) {
            this.assigned = assigned;
            this.unassignedIds = unassignedIds;
        }
    }

    /**
     * Simule l'assignation de véhicules pour toutes les réservations d'une date donnée.
     *
     * @param date la date à simuler
     * @return SimulationResult contenant les lignes assignées et les IDs non assignés
     */
    public SimulationResult simuler(LocalDate date) {
        // Récupérer les réservations du jour, triées par heure
        List<Reservation> reservations = reservationService.findByDate(date)
                .stream()
                .sorted(Comparator.comparing(Reservation::getDateHeure))
                .collect(Collectors.toList());

        // Tous les véhicules disponibles
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();

        // Paramètres de calcul
        int vitesseMoyenne = parametreService.getVitesseMoyenne();  // km/h
        int tempsAttente = parametreService.getTempsAttente();        // minutes

        Hostel aeroport = hostelRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Hôtel aéroport (id=1) non trouvé en base."));

        List<Planning> assigned = new ArrayList<>();
        List<Long> unassignedIds = new ArrayList<>();
        Set<Long> vehiculesUtilises = new HashSet<>();
        Random random = new Random();

        for (Reservation reservation : reservations) {
            int nbPassager = reservation.getNbPassager();
            Hostel hotelArrivee = reservation.getHotel();

            // Calculer les horaires
            double distanceAller = distanceService.getDistanceKm(aeroport.getId(), hotelArrivee.getId());
            double distanceRetour = distanceService.getDistanceKm(hotelArrivee.getId(), aeroport.getId());
            long tempsAllerMin = Math.round((distanceAller / vitesseMoyenne) * 60);
            long tempsRetourMin = Math.round((distanceRetour / vitesseMoyenne) * 60);

            LocalDateTime dateHeureDepart = reservation.getDateHeure();
            LocalDateTime dateHeureRetour = dateHeureDepart
                    .plusMinutes(tempsAllerMin)
                    // .plusMinutes(tempsAttente)
                    .plusMinutes(tempsRetourMin);

            // Filtrer : véhicules non utilisés avec capacite >= nbPassager
            List<Vehicule> candidats = tousVehicules.stream()
                    .filter(v -> !vehiculesUtilises.contains(v.getId()))
                    .filter(v -> v.getCapacite() >= nbPassager)
                    .collect(Collectors.toList());

            if (candidats.isEmpty()) {
                unassignedIds.add(reservation.getId());
                continue;
            }

            // Plus petite capacité suffisante
            int minCapacite = candidats.stream().mapToInt(Vehicule::getCapacite).min().getAsInt();
            List<Vehicule> plusPetits = candidats.stream()
                    .filter(v -> v.getCapacite() == minCapacite)
                    .collect(Collectors.toList());

            Vehicule choisi;
            if (plusPetits.size() == 1) {
                choisi = plusPetits.get(0);
            } else {
                // Priorité Diesel
                List<Vehicule> diesels = plusPetits.stream()
                        .filter(v -> "Diesel".equalsIgnoreCase(v.getTypeCarburant().getLibelle()))
                        .collect(Collectors.toList());
                if (!diesels.isEmpty()) {
                    // Un ou plusieurs Diesel à capacité égale → random parmi les Diesel
                    choisi = diesels.get(random.nextInt(diesels.size()));
                } else {
                    // Même capacité, même type (non-Diesel) → random
                    choisi = plusPetits.get(random.nextInt(plusPetits.size()));
                }
            }

            vehiculesUtilises.add(choisi.getId());

            Planning row = new Planning(
                    reservation.getId(),
                    nbPassager,
                    dateHeureDepart,
                    dateHeureRetour,
                    choisi,
                    distanceAller,
                    hotelArrivee.getNom()
            );
            assigned.add(row);
        }

        return new SimulationResult(assigned, unassignedIds);
    }
}
