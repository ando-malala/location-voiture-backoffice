package com.s5.framework.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.ReservationService;
import com.s5.framework.dev.services.VehiculeService;

@Controller
public class HomeController {

    private final ReservationService reservationService;
    private final HostelService hostelService;
    private final VehiculeService vehiculeService;

    @Autowired
    public HomeController(ReservationService reservationService,
                          HostelService hostelService,
                          VehiculeService vehiculeService) {
        this.reservationService = reservationService;
        this.hostelService = hostelService;
        this.vehiculeService = vehiculeService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("nbReservations", reservationService.findAll().size());
        model.addAttribute("nbHostels", hostelService.findAll().size());
        model.addAttribute("nbVehicules", vehiculeService.findAll().size());
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }
}
