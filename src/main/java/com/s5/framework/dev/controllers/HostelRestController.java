package com.s5.framework.dev.controllers;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.services.HostelService;
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
 *   GET    /api/hostels        -> Liste tous les hôtels
 *   GET    /api/hostels/{id}   -> Récupère un hôtel par ID
 *   POST   /api/hostels        -> Crée un nouvel hôtel
 */
@RestController
@RequestMapping("/api/hostels")
@CrossOrigin(origins = "*")
public class HostelRestController {

    private final HostelService hostelService;

    @Autowired
    public HostelRestController(HostelService hostelService) {
        this.hostelService = hostelService;
    }

    /**
     * GET /api/hostels
     * Retourne tous les hôtels.
     */
    @GetMapping
    public ResponseEntity<List<Hostel>> findAll() {
        List<Hostel> hostels = hostelService.findAll();
        return ResponseEntity.ok(hostels);
    }

    /**
     * GET /api/hostels/{id}
     * Retourne un hôtel par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Hostel> findById(@PathVariable Long id) {
        Hostel hostel = hostelService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hôtel non trouvé"));
        return ResponseEntity.ok(hostel);
    }

    /**
     * POST /api/hostels
     * Crée un nouvel hôtel.
     *
     * Body JSON attendu :
     * {
     *   "nom": "Hôtel Madagascar"
     * }
     */
    @PostMapping
    public ResponseEntity<Hostel> create(@RequestBody Hostel hostel) {
        Hostel saved = hostelService.create(hostel);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
