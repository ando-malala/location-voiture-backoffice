package com.s5.framework.dev.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Représente un véhicule (Diesel ou Electrique) utilisé pour les navettes aéroport → hôtel.
 * type : 'D' = Diesel, 'E' = Electrique
 */
@Entity
@Table(name = "vehicule")
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capacite", nullable = false)
    private Integer capacite;

    /** 'D' = Diesel, 'E' = Electrique */
    @Column(name = "type", nullable = false, length = 1)
    private String type;

    public Vehicule() {
    }

    public Vehicule(Long id, Integer capacite, String type) {
        this.id = id;
        this.capacite = capacite;
        this.type = type;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCapacite() { return capacite; }
    public void setCapacite(Integer capacite) { this.capacite = capacite; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicule)) return false;
        Vehicule vehicule = (Vehicule) o;
        return Objects.equals(id, vehicule.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "Vehicule{id=" + id + ", capacite=" + capacite + ", type='" + type + "'}";
    }
}
