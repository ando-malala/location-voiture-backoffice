package com.s5.framework.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.s5.framework.dev.models.Parametre;
import com.s5.framework.dev.models.Unite;
import com.s5.framework.dev.services.ParametreService;
import com.s5.framework.dev.services.UniteService;

/**
 * Contrôleur Thymeleaf pour le CRUD des Paramètres.
 */
@Controller
@RequestMapping("/parametres")
public class ParametreViewController {

    private final ParametreService parametreService;
    private final UniteService uniteService;

    @Autowired
    public ParametreViewController(ParametreService parametreService, UniteService uniteService) {
        this.parametreService = parametreService;
        this.uniteService = uniteService;
    }

    /** GET /parametres -> Liste */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("parametres", parametreService.findAll());
        model.addAttribute("activePage", "parametres");
        return "parametre/list";
    }

    /** GET /parametres/new -> Formulaire création */
    @GetMapping("/new")
    public String insertForm(Model model) {
        model.addAttribute("parametre", new Parametre());
        model.addAttribute("unites", uniteService.findAll());
        model.addAttribute("activePage", "parametres");
        return "parametre/insert";
    }

    /** POST /parametres/save -> Enregistre un nouveau paramètre */
    @PostMapping("/save")
    public String save(@RequestParam String libelle,
                       @RequestParam Integer valeur,
                       @RequestParam Long uniteId) {
        Unite unite = uniteService.findById(uniteId)
                .orElseThrow(() -> new RuntimeException("Unité non trouvée : " + uniteId));
        Parametre p = new Parametre();
        p.setLibelle(libelle);
        p.setValeur(valeur);
        p.setUnite(unite);
        parametreService.create(p);
        return "redirect:/parametres";
    }

    /** GET /parametres/edit/{id} -> Formulaire édition */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Parametre p = parametreService.findById(id)
                .orElseThrow(() -> new RuntimeException("Paramètre non trouvé : " + id));
        model.addAttribute("parametre", p);
        model.addAttribute("unites", uniteService.findAll());
        model.addAttribute("activePage", "parametres");
        return "parametre/edit";
    }

    /** POST /parametres/update -> Met à jour un paramètre */
    @PostMapping("/update")
    public String update(@RequestParam Long id,
                         @RequestParam String libelle,
                         @RequestParam Integer valeur,
                         @RequestParam Long uniteId) {
        Parametre p = parametreService.findById(id)
                .orElseThrow(() -> new RuntimeException("Paramètre non trouvé : " + id));
        Unite unite = uniteService.findById(uniteId)
                .orElseThrow(() -> new RuntimeException("Unité non trouvée : " + uniteId));
        p.setLibelle(libelle);
        p.setValeur(valeur);
        p.setUnite(unite);
        parametreService.update(p);
        return "redirect:/parametres";
    }

    /** POST /parametres/delete/{id} -> Supprime un paramètre */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        parametreService.deleteById(id);
        return "redirect:/parametres";
    }
}
