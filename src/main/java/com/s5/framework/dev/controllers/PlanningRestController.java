package com.s5.framework.dev.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Lieu;
import com.s5.framework.dev.models.Planning;
import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.models.Vehicule;
import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.LieuService;
import com.s5.framework.dev.services.PlanningService;
import com.s5.framework.dev.services.ReservationService;
import com.s5.framework.dev.services.VehiculeService;

/**
 * API REST pour les Plannings de trajets véhicules.
 *
 * Endpoints disponibles :
 *   GET    /api/plannings              -> Liste tous les plannings (triés par date départ)
 *   GET    /api/plannings/{id}         -> Récupère un planning par ID
 *   GET    /api/plannings/date/{date}  -> Récupère les plannings par date (yyyy-MM-dd)
 *   GET    /api/plannings/vehicule/{id}-> Récupère les plannings d'un véhicule
 *   POST   /api/plannings              -> Crée un nouveau planning
 */
@RestController
@RequestMapping("/api/plannings")
@CrossOrigin(origins = "*")
public class PlanningRestController {

    private final PlanningService planningService;
    private final VehiculeService vehiculeService;
    private final HostelService hostelService;
    private final LieuService lieuService;
    private final ReservationService reservationService;

    @Autowired
    public PlanningRestController(PlanningService planningService, VehiculeService vehiculeService,
                                  HostelService hostelService, LieuService lieuService,
                                  ReservationService reservationService) {
        this.planningService = planningService;
        this.vehiculeService = vehiculeService;
        this.hostelService = hostelService;
        this.lieuService = lieuService;
        this.reservationService = reservationService;
    }

    /**
     * GET /api/plannings
     * Retourne tous les plannings triés par date de départ.
     */
    @GetMapping
    public ResponseEntity<List<Planning>> findAll() {
        List<Planning> plannings = planningService.findAll();
        return ResponseEntity.ok(plannings);
    }

    /**
     * GET /api/plannings/{id}
     * Retourne un planning par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Planning> findById(@PathVariable Long id) {
        Planning planning = planningService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planning non trouvé"));
        return ResponseEntity.ok(planning);
    }

    /**
     * GET /api/plannings/date/{date}
     * Retourne les plannings pour une date donnée (format : yyyy-MM-dd).
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<Planning>> findByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Planning> plannings = planningService.findByDate(localDate);
        return ResponseEntity.ok(plannings);
    }

    /**
     * GET /api/plannings/vehicule/{vehiculeId}
     * Retourne les plannings d'un véhicule spécifique.
     */
    @GetMapping("/vehicule/{vehiculeId}")
    public ResponseEntity<List<Planning>> findByVehicule(@PathVariable Long vehiculeId) {
        List<Planning> plannings = planningService.findByVehiculeId(vehiculeId);
        return ResponseEntity.ok(plannings);
    }

    /**
     * POST /api/plannings
     * Crée un nouveau planning.
     *
     * Body JSON attendu :
     * {
     *   "vehicule": { "id": 1 },
     *   "hotel": { "id": 1 },
     *   "lieuDepart": { "id": 1 },
     *   "lieuRetour": { "id": 2 },
     *   "dateHeureDepart": "2026-03-05T09:00:00",
     *   "dateHeureRetour": "2026-03-05T10:15:00",
     *   "reservation": { "id": 1 },
     *   "statut": "PLANIFIE"
     * }
     */
    @PostMapping
    public ResponseEntity<Planning> create(@RequestBody Planning planning) {
        // Résoudre le véhicule
        if (planning.getVehicule() != null && planning.getVehicule().getId() != null) {
            Vehicule vehicule = vehiculeService.findById(planning.getVehicule().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Véhicule non trouvé"));
            planning.setVehicule(vehicule);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le véhicule est obligatoire");
        }

        // Résoudre l'hôtel
        if (planning.getHotel() != null && planning.getHotel().getId() != null) {
            Hostel hostel = hostelService.findById(planning.getHotel().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hôtel non trouvé"));
            planning.setHotel(hostel);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'hôtel est obligatoire");
        }

        // Résoudre le lieu de départ
        if (planning.getLieuDepart() != null && planning.getLieuDepart().getId() != null) {
            Lieu lieuDepart = lieuService.findById(planning.getLieuDepart().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lieu de départ non trouvé"));
            planning.setLieuDepart(lieuDepart);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lieu de départ est obligatoire");
        }

        // Résoudre le lieu de retour
        if (planning.getLieuRetour() != null && planning.getLieuRetour().getId() != null) {
            Lieu lieuRetour = lieuService.findById(planning.getLieuRetour().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lieu de retour non trouvé"));
            planning.setLieuRetour(lieuRetour);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lieu de retour est obligatoire");
        }

        // Résoudre la réservation
        if (planning.getReservation() != null && planning.getReservation().getId() != null) {
            Reservation reservation = reservationService.findById(planning.getReservation().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Réservation non trouvée"));
            planning.setReservation(reservation);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La réservation est obligatoire");
        }

        Planning saved = planningService.create(planning);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
