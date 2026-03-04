package com.s5.framework.dev.controllers;

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
import com.s5.framework.dev.services.HostelService;

/**
 * API REST pour les Hôtels.
 *
 * Endpoints disponibles :
 *   GET    /api/hostels        -> Liste tous les hôtels
 *   GET    /api/hostels/{id}   -> Récupère un hôtel par ID
 *   POST   /api/hostels        -> Crée un nouvel hôtel
 *
 * Body POST :
 * {
 *   "nom": "Hôtel Madagascar"
 * }
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
        return ResponseEntity.status(HttpStatus.CREATED).body(hostelService.create(hostel));
    }
}

