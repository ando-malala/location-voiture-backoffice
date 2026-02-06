package com.s5.framework.dev.controllers;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur Spring MVC pour les pages Thymeleaf des Réservations.
 */
@Controller
@RequestMapping("/reservations")
public class ReservationViewController {

    private final ReservationService reservationService;
    private final HostelService hostelService;

    @Autowired
    public ReservationViewController(ReservationService reservationService, HostelService hostelService) {
        this.reservationService = reservationService;
        this.hostelService = hostelService;
    }

    /**
     * GET /reservations -> Affiche la liste des réservations
     */
    @GetMapping
    public String list(@RequestParam(required = false) String date, Model model) {
        List<Reservation> reservations;
        if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            reservations = reservationService.findByDate(localDate);
            model.addAttribute("filterDate", date);
        } else {
            reservations = reservationService.findAll();
        }
        model.addAttribute("reservations", reservations);
        return "reservation/list";
    }

    /**
     * GET /reservations/new -> Affiche le formulaire d'insertion
     */
    @GetMapping("/new")
    public String insertForm(Model model) {
        List<Hostel> hostels = hostelService.findAll();
        model.addAttribute("hostels", hostels);
        model.addAttribute("reservation", new Reservation());
        return "reservation/insert";
    }

    /**
     * POST /reservations/save -> Sauvegarde une nouvelle réservation
     */
    @PostMapping("/save")
    public String save(@RequestParam String idClient,
                       @RequestParam Integer nbPassager,
                       @RequestParam String dateHeure,
                       @RequestParam Long hostelId) {
        Hostel hostel = hostelService.findById(hostelId)
                .orElseThrow(() -> new RuntimeException("Hôtel non trouvé avec l'ID : " + hostelId));

        Reservation reservation = new Reservation();
        reservation.setIdClient(idClient);
        reservation.setNbPassager(nbPassager);
        reservation.setDateHeure(LocalDate.parse(dateHeure));
        reservation.setHotel(hostel);

        reservationService.create(reservation);
        return "redirect:/reservations";
    }
}
