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

/**
 * Représente un véhicule utilisé pour les navettes aéroport → hôtel.
 */
@Entity
@Table(name = "vehicule")
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capacite", nullable = false)
    private Integer capacite;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "typecarburantid", nullable = false)
    private TypeCarburant typeCarburant;

    public Vehicule() {}

    public Vehicule(Long id, Integer capacite, TypeCarburant typeCarburant) {
        this.id = id;
        this.capacite = capacite;
        this.typeCarburant = typeCarburant;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCapacite() { return capacite; }
    public void setCapacite(Integer capacite) { this.capacite = capacite; }

    public TypeCarburant getTypeCarburant() { return typeCarburant; }
    public void setTypeCarburant(TypeCarburant typeCarburant) { this.typeCarburant = typeCarburant; }

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
        return "Vehicule{id=" + id + ", capacite=" + capacite + ", typeCarburant=" + typeCarburant + "}";
    }
}

