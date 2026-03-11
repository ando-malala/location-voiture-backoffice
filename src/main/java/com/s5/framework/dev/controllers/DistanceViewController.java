package com.s5.framework.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.s5.framework.dev.models.Distance;
import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.services.DistanceService;
import com.s5.framework.dev.services.HostelService;

/**
 * Contrôleur Thymeleaf pour le CRUD des Distances.
 */
@Controller
@RequestMapping("/distances")
public class DistanceViewController {

    private final DistanceService distanceService;
    private final HostelService hostelService;

    @Autowired
    public DistanceViewController(DistanceService distanceService, HostelService hostelService) {
        this.distanceService = distanceService;
        this.hostelService = hostelService;
    }

    /** GET /distances -> Liste */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("distances", distanceService.findAll());
        model.addAttribute("activePage", "distances");
        return "distance/list";
    }

    /** GET /distances/new -> Formulaire création */
    @GetMapping("/new")
    public String insertForm(Model model) {
        model.addAttribute("distance", new Distance());
        model.addAttribute("hostels", hostelService.findAll());
        model.addAttribute("activePage", "distances");
        return "distance/insert";
    }

    /** POST /distances/save -> Enregistre une nouvelle distance */
    @PostMapping("/save")
    public String save(@RequestParam Long hotelDepartId,
                       @RequestParam Long hotelArriveeId,
                       @RequestParam Double distanceKm) {
        Hostel depart = hostelService.findById(hotelDepartId)
                .orElseThrow(() -> new RuntimeException("Hôtel départ non trouvé : " + hotelDepartId));
        Hostel arrivee = hostelService.findById(hotelArriveeId)
                .orElseThrow(() -> new RuntimeException("Hôtel arrivée non trouvé : " + hotelArriveeId));
        Distance d = new Distance();
        d.setHotelDepart(depart);
        d.setHotelArrivee(arrivee);
        d.setDistanceKm(distanceKm);
        distanceService.create(d);
        return "redirect:/distances";
    }

    /** GET /distances/edit/{id} -> Formulaire édition */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Distance d = distanceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Distance non trouvée : " + id));
        model.addAttribute("distance", d);
        model.addAttribute("hostels", hostelService.findAll());
        model.addAttribute("activePage", "distances");
        return "distance/edit";
    }

    /** POST /distances/update -> Met à jour une distance */
    @PostMapping("/update")
    public String update(@RequestParam Long id,
                         @RequestParam Long hotelDepartId,
                         @RequestParam Long hotelArriveeId,
                         @RequestParam Double distanceKm) {
        Distance d = distanceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Distance non trouvée : " + id));
        Hostel depart = hostelService.findById(hotelDepartId)
                .orElseThrow(() -> new RuntimeException("Hôtel départ non trouvé : " + hotelDepartId));
        Hostel arrivee = hostelService.findById(hotelArriveeId)
                .orElseThrow(() -> new RuntimeException("Hôtel arrivée non trouvé : " + hotelArriveeId));
        d.setHotelDepart(depart);
        d.setHotelArrivee(arrivee);
        d.setDistanceKm(distanceKm);
        distanceService.update(d);
        return "redirect:/distances";
    }

    /** POST /distances/delete/{id} -> Supprime une distance */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        distanceService.deleteById(id);
        return "redirect:/distances";
    }
}
