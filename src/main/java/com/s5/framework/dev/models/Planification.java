package com.s5.framework.dev.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Représente une planification persistée d'un trajet (1 véhicule, 1 créneau).
 */
@Entity
@Table(name = "planification")
public class Planification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Date de la planification (jour de la simulation). */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "dateheuredepart", nullable = false)
    private LocalDateTime dateHeureDepart;

    @Column(name = "dateheureretour", nullable = false)
    private LocalDateTime dateHeureRetour;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idvehicule", nullable = false)
    private Vehicule vehicule;

    @Column(name = "combined", nullable = false)
    private boolean combined;

    @Column(name = "nbtrajet", nullable = false)
    private int nbTrajet;

    /** Liste des hôtels visités (séparés par ","). */
    @Column(name = "route_hotels", length = 1000)
    private String routeHotels;

    @ManyToMany
    @JoinTable(
            name = "planification_reservation",
            joinColumns = @JoinColumn(name = "planification_id"),
            inverseJoinColumns = @JoinColumn(name = "reservation_id")
    )
    private List<Reservation> reservations = new ArrayList<>();

    public Planification() {
    }

    public Planification(LocalDate date,
                        LocalDateTime dateHeureDepart,
                        LocalDateTime dateHeureRetour,
                        Vehicule vehicule,
                        boolean combined,
                        int nbTrajet,
                        String routeHotels,
                        List<Reservation> reservations) {
        this.date = date;
        this.dateHeureDepart = dateHeureDepart;
        this.dateHeureRetour = dateHeureRetour;
        this.vehicule = vehicule;
        this.combined = combined;
        this.nbTrajet = nbTrajet;
        this.routeHotels = routeHotels;
        this.reservations = reservations != null ? reservations : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public boolean isCombined() {
        return combined;
    }

    public void setCombined(boolean combined) {
        this.combined = combined;
    }

    public int getNbTrajet() {
        return nbTrajet;
    }

    public void setNbTrajet(int nbTrajet) {
        this.nbTrajet = nbTrajet;
    }

    public String getRouteHotels() {
        return routeHotels;
    }

    public void setRouteHotels(String routeHotels) {
        this.routeHotels = routeHotels;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }
}
