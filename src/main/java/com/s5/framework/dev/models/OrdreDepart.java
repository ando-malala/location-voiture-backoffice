package com.s5.framework.dev.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.Objects;

/** Ordre de départ d'un véhicule depuis l'aéroport pour une réservation donnée. */
@Entity
@Table(name = "ordredepart")
public class OrdreDepart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idvehicule", nullable = false)
    private Vehicule vehicule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idreservation", nullable = false)
    private Reservation reservation;

    /** Heure de départ du véhicule de l'aéroport. */
    @Column(name = "heuredepart", nullable = false)
    private LocalTime heureDepart;

    public OrdreDepart() {
    }

    public OrdreDepart(Long id, Vehicule vehicule, Reservation reservation, LocalTime heureDepart) {
        this.id = id;
        this.vehicule = vehicule;
        this.reservation = reservation;
        this.heureDepart = heureDepart;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vehicule getVehicule() { return vehicule; }
    public void setVehicule(Vehicule vehicule) { this.vehicule = vehicule; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime heureDepart) { this.heureDepart = heureDepart; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdreDepart)) return false;
        OrdreDepart that = (OrdreDepart) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "OrdreDepart{id=" + id + ", heureDepart=" + heureDepart + '}';
    }
}
