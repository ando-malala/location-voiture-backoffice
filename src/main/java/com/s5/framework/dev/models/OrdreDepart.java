package com.s5.framework.dev.models;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Ancien ordre de départ — table supprimée, classe conservée comme POJO pour compatibilité.
 * La planification est désormais une simulation pure (non persistée).
 */
public class OrdreDepart {

    private Long id;
    private Vehicule vehicule;
    private Reservation reservation;
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
