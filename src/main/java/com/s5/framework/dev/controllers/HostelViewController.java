package com.s5.framework.dev.controllers;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.services.HostelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Contrôleur Spring MVC pour les pages Thymeleaf des Hôtels.
 */
@Controller
@RequestMapping("/hostels")
public class HostelViewController {

    private final HostelService hostelService;

    @Autowired
    public HostelViewController(HostelService hostelService) {
        this.hostelService = hostelService;
    }

    /**
     * GET /hostels -> Affiche la liste des hôtels
     */
    @GetMapping
    public String list(Model model) {
        List<Hostel> hostels = hostelService.findAll();
        model.addAttribute("hostels", hostels);
        return "hostel/list";
    }

    /**
     * GET /hostels/new -> Affiche le formulaire d'insertion
     */
    @GetMapping("/new")
    public String insertForm(Model model) {
        model.addAttribute("hostel", new Hostel());
        return "hostel/insert";
    }

    /**
     * POST /hostels/save -> Sauvegarde un nouvel hôtel
     */
    @PostMapping("/save")
    public String save(@RequestParam String nom) {
        Hostel hostel = new Hostel();
        hostel.setNom(nom);

        hostelService.create(hostel);
        return "redirect:/hostels";
    }
}
