package com.s5.framework.dev.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.s5.framework.dev.services.AssignmentService;
import com.s5.framework.dev.services.AssignmentService.SimulationResult;

/**
 * Contrôleur Spring MVC pour la page Planification (simulation statique).
 * Aucune persistance — calcul à la volée selon la date choisie.
 */
@Controller
@RequestMapping("/planification")
public class PlanningViewController {

    private final AssignmentService assignmentService;

    @Autowired
    public PlanningViewController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * GET /planification                  → page avec sélecteur de date
     * GET /planification?date=yyyy-MM-dd  → simulation pour la date donnée
     */
    @GetMapping
    public String planification(@RequestParam(required = false) String date, Model model) {
        model.addAttribute("activePage", "planification");

        if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            model.addAttribute("filterDate", date);
            try {
                SimulationResult result = assignmentService.simuler(localDate);
                model.addAttribute("assigned", result.assigned);
                model.addAttribute("unassigned", result.unassignedIds);
            } catch (RuntimeException e) {
                model.addAttribute("errorMessage", "Erreur lors de la simulation : " + e.getMessage());
                model.addAttribute("assigned", List.of());
                model.addAttribute("unassigned", List.of());
            }
        } else {
            model.addAttribute("assigned", null);
            model.addAttribute("unassigned", null);
        }

        return "planification/list";
    }
}
