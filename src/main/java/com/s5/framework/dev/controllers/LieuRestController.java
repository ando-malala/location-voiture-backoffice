package com.s5.framework.dev.controllers;

import com.s5.framework.dev.models.Lieu;
import com.s5.framework.dev.services.LieuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API REST pour les Lieux.
 *
 * Endpoints disponibles :
 *   GET    /api/lieux        -> Liste tous les lieux
 *   GET    /api/lieux/{id}   -> Récupère un lieu par ID
 *   POST   /api/lieux        -> Crée un nouveau lieu
 *
 * Body POST :
 * {
 *   "code": "ARP_TN",
 *   "nom": "Aéroport Ivato"
 * }
 */
@RestController
@RequestMapping("/api/lieux")
@CrossOrigin(origins = "*")
public class LieuRestController {

    private final LieuService lieuService;

    @Autowired
    public LieuRestController(LieuService lieuService) {
        this.lieuService = lieuService;
    }

    /** GET /api/lieux */
    @GetMapping
    public ResponseEntity<List<Lieu>> findAll() {
        return ResponseEntity.ok(lieuService.findAll());
    }

    /** GET /api/lieux/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Lieu> findById(@PathVariable Long id) {
        Lieu lieu = lieuService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lieu non trouvé"));
        return ResponseEntity.ok(lieu);
    }

    /** POST /api/lieux */
    @PostMapping
    public ResponseEntity<Lieu> create(@RequestBody Lieu lieu) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lieuService.create(lieu));
    }
}
