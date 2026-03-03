package com.s5.framework.dev.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.models.Planning;
import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.services.AssignmentService;
import com.s5.framework.dev.services.HostelService;
import com.s5.framework.dev.services.ReservationService;

/**
 * Contrôleur Spring MVC pour les pages Thymeleaf des Réservations.
 */
@Controller
@RequestMapping("/reservations")
public class ReservationViewController {

    private final ReservationService reservationService;
    private final HostelService hostelService;
    private final AssignmentService assignmentService;

    @Autowired
    public ReservationViewController(ReservationService reservationService, HostelService hostelService,
                                     AssignmentService assignmentService) {
        this.reservationService = reservationService;
        this.hostelService = hostelService;
        this.assignmentService = assignmentService;
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
        model.addAttribute("activePage", "reservations");
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
        model.addAttribute("activePage", "reservations");
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
        // Formulaire envoie yyyy-MM-dd, on stocke en début de journée
        reservation.setDateHeure(LocalDate.parse(dateHeure).atStartOfDay());
        reservation.setHotel(hostel);

        reservationService.create(reservation);
        return "redirect:/reservations";
    }

    /**
     * POST /reservations/assign -> Assignation automatique d'un véhicule à une réservation.
     * Crée un planning avec calcul automatique des horaires et sélection du véhicule.
     */
    @PostMapping("/assign")
    public String assign(@RequestParam Long reservationId,
                         @RequestParam String dateHeureDepart,
                         RedirectAttributes redirectAttributes) {
        try {
            Reservation reservation = reservationService.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Réservation #" + reservationId + " non trouvée."));

            LocalDateTime depart = LocalDateTime.parse(dateHeureDepart);
            List<Planning> plannings = assignmentService.assignerAutomatiquement(reservation, depart);

            if (plannings.size() == 1) {
                Planning p = plannings.get(0);
                redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Réservation #" + reservationId + " assignée au Véhicule #" + p.getVehicule().getId()
                        + " (" + p.getVehicule().getCapacite() + " places, "
                        + p.getVehicule().getTypeCarburant().getLibelle() + ")"
                        + " — " + p.getNbPassagers() + " passager(s) transportés"
                        + " — Départ: " + p.getDateHeureDepart()
                        + ", Retour: " + p.getDateHeureRetour());
            } else {
                String vehiculesInfo = plannings.stream()
                        .map(p -> "Véhicule #" + p.getVehicule().getId()
                                + " (" + p.getNbPassagers() + "/" + p.getVehicule().getCapacite() + " places, "
                                + p.getVehicule().getTypeCarburant().getLibelle() + ")")
                        .collect(Collectors.joining(" + "));
                int totalTransportes = plannings.stream()
                        .mapToInt(Planning::getNbPassagers).sum();
                redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Réservation #" + reservationId + " répartie sur " + plannings.size()
                        + " véhicules : " + vehiculesInfo
                        + " — Total: " + totalTransportes + " passager(s) transportés"
                        + " — Départ: " + plannings.get(0).getDateHeureDepart()
                        + ", Retour: " + plannings.get(0).getDateHeureRetour());
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/reservations";
    }
}
