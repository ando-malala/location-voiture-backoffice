package com.s5.framework.dev.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/** Unité de mesure utilisée pour les paramètres (ex: km/h, minutes...). */
@Entity
@Table(name = "unite")
public class Unite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "libelle", length = 55)
    private String libelle;

    public Unite() {
    }

    public Unite(Long id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unite)) return false;
        Unite unite = (Unite) o;
        return Objects.equals(id, unite.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "Unite{id=" + id + ", libelle='" + libelle + "'}";
    }
}
