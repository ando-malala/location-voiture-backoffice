package com.s5.framework.dev.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.s5.framework.dev.models.TypeCarburant;
import com.s5.framework.dev.models.Vehicule;
import com.s5.framework.dev.services.TypeCarburantService;
import com.s5.framework.dev.services.VehiculeService;

/**
 * Contrôleur Thymeleaf pour le CRUD des Véhicules.
 */
@Controller
@RequestMapping("/vehicules")
public class VehiculeViewController {

    private final VehiculeService vehiculeService;
    private final TypeCarburantService typeCarburantService;

    @Autowired
    public VehiculeViewController(VehiculeService vehiculeService, TypeCarburantService typeCarburantService) {
        this.vehiculeService = vehiculeService;
        this.typeCarburantService = typeCarburantService;
    }

    /** GET /vehicules -> Liste */
    @GetMapping
    public String list(Model model) {
        List<Vehicule> vehicules = vehiculeService.findAll();
        model.addAttribute("vehicules", vehicules);
        model.addAttribute("activePage", "vehicules");
        return "vehicule/list";
    }

    /** GET /vehicules/new -> Formulaire création */
    @GetMapping("/new")
    public String insertForm(Model model) {
        model.addAttribute("vehicule", new Vehicule());
        model.addAttribute("typesCarburant", typeCarburantService.findAll());
        model.addAttribute("activePage", "vehicules");
        return "vehicule/insert";
    }

    /** POST /vehicules/save -> Enregistre un nouveau véhicule */
    @PostMapping("/save")
    public String save(@RequestParam String reference,
                       @RequestParam Integer capacite,
                       @RequestParam Long typeCarburantId) {
        TypeCarburant tc = typeCarburantService.findById(typeCarburantId)
                .orElseThrow(() -> new RuntimeException("Type de carburant non trouvé : " + typeCarburantId));
        Vehicule v = new Vehicule();
        v.setReference(reference);
        v.setCapacite(capacite);
        v.setTypeCarburant(tc);
        vehiculeService.create(v);
        return "redirect:/vehicules";
    }

    /** GET /vehicules/edit/{id} -> Formulaire édition */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Vehicule v = vehiculeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + id));
        model.addAttribute("vehicule", v);
        model.addAttribute("typesCarburant", typeCarburantService.findAll());
        model.addAttribute("activePage", "vehicules");
        return "vehicule/edit";
    }

    /** POST /vehicules/update -> Met à jour un véhicule */
    @PostMapping("/update")
    public String update(@RequestParam Long id,
                         @RequestParam String reference,
                         @RequestParam Integer capacite,
                         @RequestParam Long typeCarburantId) {
        TypeCarburant tc = typeCarburantService.findById(typeCarburantId)
                .orElseThrow(() -> new RuntimeException("Type de carburant non trouvé : " + typeCarburantId));
        Vehicule v = vehiculeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + id));
        v.setReference(reference);
        v.setCapacite(capacite);
        v.setTypeCarburant(tc);
        vehiculeService.update(v);
        return "redirect:/vehicules";
    }

    /** POST /vehicules/delete/{id} -> Supprime un véhicule */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        vehiculeService.deleteById(id);
        return "redirect:/vehicules";
    }
}
