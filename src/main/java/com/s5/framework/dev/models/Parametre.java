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

/** Paramètre de configuration (ex: vitesse moyenne, temps d'attente) avec son unité. */
@Entity
@Table(name = "parametre")
public class Parametre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "libelle", nullable = false, length = 255)
    private String libelle;

    @Column(name = "valeur", nullable = false)
    private Integer valeur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idunite", nullable = false)
    private Unite unite;

    public Parametre() {
    }

    public Parametre(Long id, String libelle, Integer valeur, Unite unite) {
        this.id = id;
        this.libelle = libelle;
        this.valeur = valeur;
        this.unite = unite;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public Integer getValeur() { return valeur; }
    public void setValeur(Integer valeur) { this.valeur = valeur; }

    public Unite getUnite() { return unite; }
    public void setUnite(Unite unite) { this.unite = unite; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parametre)) return false;
        Parametre that = (Parametre) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "Parametre{id=" + id + ", libelle='" + libelle + "', valeur=" + valeur + '}';
    }
}
