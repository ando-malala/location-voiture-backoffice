package com.s5.framework.dev.models;

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

/** Distance en km entre deux hôtels (départ → arrivée). Le point de départ est toujours l'aéroport (hotel id=1). */
@Entity
@Table(name = "distance")
public class Distance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idlieudepart", nullable = false)
    private Hostel hotelDepart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idlieuarrivee", nullable = false)
    private Hostel hotelArrivee;

    @Column(name = "distancekm", nullable = false)
    private Double distanceKm;

    public Distance() {
    }

    public Distance(Long id, Hostel hotelDepart, Hostel hotelArrivee, Double distanceKm) {
        this.id = id;
        this.hotelDepart = hotelDepart;
        this.hotelArrivee = hotelArrivee;
        this.distanceKm = distanceKm;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Hostel getHotelDepart() { return hotelDepart; }
    public void setHotelDepart(Hostel hotelDepart) { this.hotelDepart = hotelDepart; }

    public Hostel getHotelArrivee() { return hotelArrivee; }
    public void setHotelArrivee(Hostel hotelArrivee) { this.hotelArrivee = hotelArrivee; }

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
