package com.s5.framework.dev.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Lieu;
import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.LieuService;

/**
 * Contrôleur Spring MVC pour les pages Thymeleaf des Hôtels.
 */
@Controller
@RequestMapping("/hostels")
public class HostelViewController {

    private final HostelService hostelService;
    private final LieuService lieuService;

    @Autowired
    public HostelViewController(HostelService hostelService, LieuService lieuService) {
        this.hostelService = hostelService;
        this.lieuService = lieuService;
    }

    /**
     * GET /hostels -> Affiche la liste des hôtels
     */
    @GetMapping
    public String list(Model model) {
        List<Hostel> hostels = hostelService.findAll();
        model.addAttribute("hostels", hostels);
        model.addAttribute("activePage", "hostels");
        return "hostel/list";
    }

    /**
     * GET /hostels/new -> Affiche le formulaire d'insertion
     */
    @GetMapping("/new")
    public String insertForm(Model model) {
        model.addAttribute("hostel", new Hostel());
        model.addAttribute("lieux", lieuService.findAll());
        model.addAttribute("activePage", "hostels");
        return "hostel/insert";
    }

    /**
     * POST /hostels/save -> Sauvegarde un nouvel hôtel
     */
    @PostMapping("/save")
    public String save(@RequestParam String nom, @RequestParam Long lieuId) {
        Lieu lieu = lieuService.findById(lieuId)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'ID : " + lieuId));
        Hostel hostel = new Hostel();
        hostel.setNom(nom);
        hostel.setLieu(lieu);
        hostelService.create(hostel);
        return "redirect:/hostels";
    }
}

