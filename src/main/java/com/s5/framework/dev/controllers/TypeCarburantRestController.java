package com.s5.framework.dev.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.s5.framework.dev.models.TypeCarburant;
import com.s5.framework.dev.services.TypeCarburantService;

/**
 * API REST pour les Types de Carburant.
 *
 * GET    /api/typecarburants        -> Liste tous
 * GET    /api/typecarburants/{id}   -> Par ID
 * POST   /api/typecarburants        -> Crée
 * DELETE /api/typecarburants/{id}   -> Supprime
 */
@RestController
@RequestMapping("/api/typecarburants")
@CrossOrigin(origins = "*")
public class TypeCarburantRestController {

    private final TypeCarburantService typeCarburantService;

    @Autowired
    public TypeCarburantRestController(TypeCarburantService typeCarburantService) {
        this.typeCarburantService = typeCarburantService;
    }

    @GetMapping
    public ResponseEntity<List<TypeCarburant>> findAll() {
        return ResponseEntity.ok(typeCarburantService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeCarburant> findById(@PathVariable Long id) {
        TypeCarburant tc = typeCarburantService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de carburant non trouvé"));
        return ResponseEntity.ok(tc);
    }

    @PostMapping
    public ResponseEntity<TypeCarburant> create(@RequestBody TypeCarburant typeCarburant) {
        return ResponseEntity.status(HttpStatus.CREATED).body(typeCarburantService.create(typeCarburant));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        typeCarburantService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de carburant non trouvé"));
        typeCarburantService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
