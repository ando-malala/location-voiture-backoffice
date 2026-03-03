package com.s5.framework.dev.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Lieu;
import com.s5.framework.dev.models.Planning;
import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.models.Vehicule;
import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.LieuService;
import com.s5.framework.dev.services.PlanningService;
import com.s5.framework.dev.services.ReservationService;
import com.s5.framework.dev.services.VehiculeService;

/**
 * Contrôleur Spring MVC pour les pages Thymeleaf du Planning.
 */
@Controller
@RequestMapping("/plannings")
public class PlanningViewController {

    private final PlanningService planningService;
    private final VehiculeService vehiculeService;
    private final HostelService hostelService;
    private final LieuService lieuService;
    private final ReservationService reservationService;

    @Autowired
    public PlanningViewController(PlanningService planningService, VehiculeService vehiculeService,
                                  HostelService hostelService, LieuService lieuService,
                                  ReservationService reservationService) {
        this.planningService = planningService;
        this.vehiculeService = vehiculeService;
        this.hostelService = hostelService;
        this.lieuService = lieuService;
        this.reservationService = reservationService;
    }

    /**
     * GET /plannings -> Affiche la liste de tous les plannings, filtrable par date.
     */
    @GetMapping
    public String list(@RequestParam(required = false) String date, Model model) {
        List<Planning> plannings;
        if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            plannings = planningService.findByDate(localDate);
            model.addAttribute("filterDate", date);
        } else {
            plannings = planningService.findAll();
        }
        // Regrouper par véhicule (LinkedHashMap pour garder l'ordre)
        Map<String, List<Planning>> planningsParVehicule = plannings.stream()
                .collect(Collectors.groupingBy(
                        p -> "Véhicule #" + p.getVehicule().getId() + " (" + p.getVehicule().getCapacite() + " places)",
                        LinkedHashMap::new,
                        Collectors.toList()));

        model.addAttribute("planningsParVehicule", planningsParVehicule);
        model.addAttribute("totalPlannings", plannings.size());
        model.addAttribute("activePage", "plannings");
        return "planning/list";
    }

    /**
     * GET /plannings/new -> Affiche le formulaire de création d'un planning
     */
    @GetMapping("/new")
    public String insertForm(Model model) {
        List<Vehicule> vehicules = vehiculeService.findAll();
        List<Hostel> hostels = hostelService.findAll();
        List<Lieu> lieux = lieuService.findAll();
        List<Reservation> reservations = reservationService.findAll();
        model.addAttribute("vehicules", vehicules);
        model.addAttribute("hostels", hostels);
        model.addAttribute("lieux", lieux);
        model.addAttribute("reservations", reservations);
        model.addAttribute("planning", new Planning());
        model.addAttribute("activePage", "plannings");
        return "planning/insert";
    }

    /**
     * POST /plannings/save -> Sauvegarde un nouveau planning
     */
    @PostMapping("/save")
    public String save(@RequestParam Long vehiculeId,
                       @RequestParam Long hostelId,
                       @RequestParam Long lieuDepartId,
                       @RequestParam Long lieuRetourId,
                       @RequestParam String dateHeureDepart,
                       @RequestParam(required = false) String heureArriveeHotel,
                       @RequestParam(required = false) String heureDepartHotel,
                       @RequestParam String dateHeureRetour,
                       @RequestParam Long reservationId,
                       @RequestParam(defaultValue = "0") int nbPassagers,
                       @RequestParam(defaultValue = "PLANIFIE") String statut) {
        Vehicule vehicule = vehiculeService.findById(vehiculeId)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé avec l'ID : " + vehiculeId));
        Hostel hostel = hostelService.findById(hostelId)
                .orElseThrow(() -> new RuntimeException("Hôtel non trouvé avec l'ID : " + hostelId));
        Lieu lieuDepart = lieuService.findById(lieuDepartId)
                .orElseThrow(() -> new RuntimeException("Lieu de départ non trouvé avec l'ID : " + lieuDepartId));
        Lieu lieuRetour = lieuService.findById(lieuRetourId)
                .orElseThrow(() -> new RuntimeException("Lieu de retour non trouvé avec l'ID : " + lieuRetourId));
        Reservation reservation = reservationService.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée avec l'ID : " + reservationId));

        Planning planning = new Planning();
        planning.setVehicule(vehicule);
        planning.setHotel(hostel);
        planning.setLieuDepart(lieuDepart);
        planning.setLieuRetour(lieuRetour);
        planning.setDateHeureDepart(LocalDateTime.parse(dateHeureDepart));
        if (heureArriveeHotel != null && !heureArriveeHotel.isEmpty()) {
            planning.setHeureArriveeHotel(LocalDateTime.parse(heureArriveeHotel));
        }
        if (heureDepartHotel != null && !heureDepartHotel.isEmpty()) {
            planning.setHeureDepartHotel(LocalDateTime.parse(heureDepartHotel));
        }
        planning.setDateHeureRetour(LocalDateTime.parse(dateHeureRetour));
        planning.setReservation(reservation);
        planning.setNbPassagers(nbPassagers > 0 ? nbPassagers : reservation.getNbPassager());
        planning.setStatut(statut);

        planningService.create(planning);
        return "redirect:/plannings";
    }
}
