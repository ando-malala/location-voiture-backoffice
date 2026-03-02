package com.s5.framework.dev.controllers;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Lieu;
import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.LieuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API REST pour les Hôtels.
 *
 * Endpoints disponibles :
 *   GET    /api/hostels        -> Liste tous les hôtels (avec leur lieu)
 *   GET    /api/hostels/{id}   -> Récupère un hôtel par ID
 *   POST   /api/hostels        -> Crée un nouvel hôtel
 *
 * Body POST :
 * {
 *   "nom": "Hôtel Madagascar",
 *   "lieu": { "id": 1 }
 * }
 */
@RestController
@RequestMapping("/api/hostels")
@CrossOrigin(origins = "*")
public class HostelRestController {

    private final HostelService hostelService;
    private final LieuService lieuService;

    @Autowired
    public HostelRestController(HostelService hostelService, LieuService lieuService) {
        this.hostelService = hostelService;
        this.lieuService = lieuService;
    }

    /** GET /api/hostels */
    @GetMapping
    public ResponseEntity<List<Hostel>> findAll() {
        return ResponseEntity.ok(hostelService.findAll());
    }

    /** GET /api/hostels/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Hostel> findById(@PathVariable Long id) {
        Hostel hostel = hostelService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hôtel non trouvé"));
        return ResponseEntity.ok(hostel);
    }

    /** POST /api/hostels */
    @PostMapping
    public ResponseEntity<Hostel> create(@RequestBody Hostel hostel) {
        if (hostel.getLieu() != null && hostel.getLieu().getId() != null) {
            Lieu lieu = lieuService.findById(hostel.getLieu().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lieu non trouvé"));
            hostel.setLieu(lieu);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lieu de l'hôtel est obligatoire");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(hostelService.create(hostel));
    }
}

