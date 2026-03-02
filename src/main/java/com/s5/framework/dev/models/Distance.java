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
import java.util.Objects;

/** Distance en km entre deux lieux (départ → arrivée). */
@Entity
@Table(name = "distance")
public class Distance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idlieudepart", nullable = false)
    private Lieu lieuDepart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idlieuarrivee", nullable = false)
    private Lieu lieuArrivee;

    @Column(name = "distancekm", nullable = false)
    private Double distanceKm;

    public Distance() {
    }

    public Distance(Long id, Lieu lieuDepart, Lieu lieuArrivee, Double distanceKm) {
        this.id = id;
        this.lieuDepart = lieuDepart;
        this.lieuArrivee = lieuArrivee;
        this.distanceKm = distanceKm;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lieu getLieuDepart() { return lieuDepart; }
    public void setLieuDepart(Lieu lieuDepart) { this.lieuDepart = lieuDepart; }

    public Lieu getLieuArrivee() { return lieuArrivee; }
    public void setLieuArrivee(Lieu lieuArrivee) { this.lieuArrivee = lieuArrivee; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Distance)) return false;
        Distance distance = (Distance) o;
        return Objects.equals(id, distance.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "Distance{id=" + id + ", distanceKm=" + distanceKm + '}';
    }
}
