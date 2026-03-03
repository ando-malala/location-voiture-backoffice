package com.s5.framework.dev.models;

import java.time.LocalDateTime;
import java.util.Objects;

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
 * Représente un planning de trajet d'un véhicule : départ, hôtel destination, retour.
 */
@Entity
@Table(name = "planning")
public class Planning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idvehicule", nullable = false)
    private Vehicule vehicule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idhotel", nullable = false)
    private Hostel hotel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idlieudepart", nullable = false)
    private Lieu lieuDepart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idlieuretour", nullable = false)
    private Lieu lieuRetour;

    /** Date et heure de départ du véhicule. */
    @Column(name = "dateheuredepart", nullable = false)
    private LocalDateTime dateHeureDepart;

    /** Date et heure d'arrivée à l'hôtel. */
    @Column(name = "heurearriveehotel")
    private LocalDateTime heureArriveeHotel;

    /** Date et heure de départ de l'hôtel. */
    @Column(name = "heuredeparthotel")
    private LocalDateTime heureDepartHotel;

    /** Date et heure de retour du véhicule. */
    @Column(name = "dateheureretour", nullable = false)
    private LocalDateTime dateHeureRetour;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idreservation", nullable = false)
    private Reservation reservation;

    /** Nombre de passagers effectivement transportés dans ce véhicule. */
    @Column(name = "nbpassagers", nullable = false)
    private int nbPassagers;

    /** Statut du trajet : PLANIFIE, EN_COURS, TERMINE. */
    @Column(name = "statut", nullable = false, length = 30)
    private String statut;

    public Planning() {
    }

    public Planning(Long id, Vehicule vehicule, Hostel hotel, Lieu lieuDepart, Lieu lieuRetour,
                    LocalDateTime dateHeureDepart, LocalDateTime heureArriveeHotel,
                    LocalDateTime heureDepartHotel, LocalDateTime dateHeureRetour,
                    Reservation reservation, int nbPassagers, String statut) {
        this.id = id;
        this.vehicule = vehicule;
        this.hotel = hotel;
        this.lieuDepart = lieuDepart;
        this.lieuRetour = lieuRetour;
        this.dateHeureDepart = dateHeureDepart;
        this.heureArriveeHotel = heureArriveeHotel;
        this.heureDepartHotel = heureDepartHotel;
        this.dateHeureRetour = dateHeureRetour;
        this.reservation = reservation;
        this.nbPassagers = nbPassagers;
        this.statut = statut;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vehicule getVehicule() { return vehicule; }
    public void setVehicule(Vehicule vehicule) { this.vehicule = vehicule; }

    public Hostel getHotel() { return hotel; }
    public void setHotel(Hostel hotel) { this.hotel = hotel; }

    public Lieu getLieuDepart() { return lieuDepart; }
    public void setLieuDepart(Lieu lieuDepart) { this.lieuDepart = lieuDepart; }

    public Lieu getLieuRetour() { return lieuRetour; }
    public void setLieuRetour(Lieu lieuRetour) { this.lieuRetour = lieuRetour; }

    public LocalDateTime getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(LocalDateTime dateHeureDepart) { this.dateHeureDepart = dateHeureDepart; }

    public LocalDateTime getHeureArriveeHotel() { return heureArriveeHotel; }
    public void setHeureArriveeHotel(LocalDateTime heureArriveeHotel) { this.heureArriveeHotel = heureArriveeHotel; }

    public LocalDateTime getHeureDepartHotel() { return heureDepartHotel; }
    public void setHeureDepartHotel(LocalDateTime heureDepartHotel) { this.heureDepartHotel = heureDepartHotel; }

    public LocalDateTime getDateHeureRetour() { return dateHeureRetour; }
    public void setDateHeureRetour(LocalDateTime dateHeureRetour) { this.dateHeureRetour = dateHeureRetour; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public int getNbPassagers() { return nbPassagers; }
    public void setNbPassagers(int nbPassagers) { this.nbPassagers = nbPassagers; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Planning)) return false;
        Planning planning = (Planning) o;
        return Objects.equals(id, planning.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "Planning{id=" + id + ", vehicule=" + vehicule + ", hotel=" + hotel
                + ", depart=" + dateHeureDepart + ", retour=" + dateHeureRetour
                + ", nbPassagers=" + nbPassagers + ", statut='" + statut + "'}";
    }
}
