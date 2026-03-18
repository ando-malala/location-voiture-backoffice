package com.s5.framework.dev.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ligne persistée de planification d'un trajet véhicule pour une journée.
 */
@Entity
@Table(name = "planification")
public class Planification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datejour", nullable = false)
    private LocalDate dateJour;

    @Column(name = "dateheuredepart", nullable = false)
    private LocalDateTime dateHeureDepart;

    @Column(name = "dateheureretour", nullable = false)
    private LocalDateTime dateHeureRetour;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idvehicule", nullable = false)
    private Vehicule vehicule;

    @Column(name = "combinedtrip", nullable = false)
    private boolean combinedTrip;

    @Column(name = "reservationsjson", columnDefinition = "TEXT")
    private String reservationsJson;

    @Column(name = "routehotelsjson", columnDefinition = "TEXT")
    private String routeHotelsJson;

    public Planification() {
    }

    public Planification(LocalDate dateJour,
                        LocalDateTime dateHeureDepart,
                        LocalDateTime dateHeureRetour,
                        Vehicule vehicule,
                        boolean combinedTrip,
                        String reservationsJson,
                        String routeHotelsJson) {
        this.dateJour = dateJour;
        this.dateHeureDepart = dateHeureDepart;
        this.dateHeureRetour = dateHeureRetour;
        this.vehicule = vehicule;
        this.combinedTrip = combinedTrip;
        this.reservationsJson = reservationsJson;
        this.routeHotelsJson = routeHotelsJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateJour() {
        return dateJour;
    }

    public void setDateJour(LocalDate dateJour) {
        this.dateJour = dateJour;
    }

    public LocalDateTime getDateHeureDepart() {
        return dateHeureDepart;
    }

    public void setDateHeureDepart(LocalDateTime dateHeureDepart) {
        this.dateHeureDepart = dateHeureDepart;
    }

    public LocalDateTime getDateHeureRetour() {
        return dateHeureRetour;
    }

    public void setDateHeureRetour(LocalDateTime dateHeureRetour) {
        this.dateHeureRetour = dateHeureRetour;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    public boolean isCombinedTrip() {
        return combinedTrip;
    }

    public void setCombinedTrip(boolean combinedTrip) {
        this.combinedTrip = combinedTrip;
    }

    public String getReservationsJson() {
        return reservationsJson;
    }

    public void setReservationsJson(String reservationsJson) {
        this.reservationsJson = reservationsJson;
    }

    public String getRouteHotelsJson() {
        return routeHotelsJson;
    }

    public void setRouteHotelsJson(String routeHotelsJson) {
        this.routeHotelsJson = routeHotelsJson;
    }
}
