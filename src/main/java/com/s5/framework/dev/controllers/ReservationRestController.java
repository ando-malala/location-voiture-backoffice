package com.s5.framework.dev.controllers;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * API REST pour les Réservations.
 *
 * Endpoints disponibles :
 *   GET    /api/reservations              -> Liste toutes les réservations
 *   GET    /api/reservations/{id}         -> Récupère une réservation par ID
 *   GET    /api/reservations/date/{date}  -> Récupère les réservations par date (yyyy-MM-dd)
 *   POST   /api/reservations              -> Crée une nouvelle réservation
 */
@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationRestController {

    private final ReservationService reservationService;
    private final HostelService hostelService;

    @Autowired
    public ReservationRestController(ReservationService reservationService, HostelService hostelService) {
        this.reservationService = reservationService;
        this.hostelService = hostelService;
    }

    /**
     * GET /api/reservations
     * Retourne toutes les réservations.
     */
    @GetMapping
    public ResponseEntity<List<Reservation>> findAll() {
        List<Reservation> reservations = reservationService.findAll();
        return ResponseEntity.ok(reservations);
    }

    /**
     * GET /api/reservations/{id}
     * Retourne une réservation par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> findById(@PathVariable Long id) {
        Reservation reservation = reservationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation non trouvée"));
        return ResponseEntity.ok(reservation);
    }

    /**
     * GET /api/reservations/date/{date}
     * Retourne les réservations pour une date donnée (format : yyyy-MM-dd).
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<Reservation>> findByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Reservation> reservations = reservationService.findByDate(localDate);
        return ResponseEntity.ok(reservations);
    }

    /**
     * POST /api/reservations
     * Crée une nouvelle réservation.
     *
     * Body JSON attendu :
     * {
     *   "idClient": "C001",
     *   "nbPassager": 3,
     *   "dateHeure": "2026-02-10",
     *   "hotel": { "id": 1 }
     * }
     */
    @PostMapping
    public ResponseEntity<Reservation> create(@RequestBody Reservation reservation) {
        // Résoudre l'hôtel depuis la base de données
        if (reservation.getHotel() != null && reservation.getHotel().getId() != null) {
            Hostel hostel = hostelService.findById(reservation.getHotel().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hôtel non trouvé"));
            reservation.setHotel(hostel);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'hôtel est obligatoire");
        }

        Reservation saved = reservationService.create(reservation);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
