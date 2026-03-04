package com.s5.framework.dev.models;

import java.time.LocalDateTime;

/**
 * DTO de simulation de planification (non persisté en base).
 * Représente l'assignation simulée d'un véhicule à une réservation pour une date donnée.
 */
public class Planning {

    private Long idReservation;
    private Integer nbPassager;
    /** Heure de départ du véhicule de l'aéroport (= dateHeure de la réservation). */
    private LocalDateTime dateHeureDepart;
    /** Heure de retour du véhicule à l'aéroport. */
    private LocalDateTime dateHeureRetour;
    private Vehicule vehicule;
    /** Distance aller aéroport → hôtel en km. */
    private Double distanceKm;
    /** Nom de l'hôtel de destination. */
    private String nomHotel;

    public Planning() {}

    public Planning(Long idReservation, Integer nbPassager,
                    LocalDateTime dateHeureDepart, LocalDateTime dateHeureRetour,
                    Vehicule vehicule, Double distanceKm, String nomHotel) {
        this.idReservation = idReservation;
        this.nbPassager = nbPassager;
        this.dateHeureDepart = dateHeureDepart;
        this.dateHeureRetour = dateHeureRetour;
        this.vehicule = vehicule;
        this.distanceKm = distanceKm;
        this.nomHotel = nomHotel;
    }

    public Long getIdReservation() { return idReservation; }
    public void setIdReservation(Long idReservation) { this.idReservation = idReservation; }

    public Integer getNbPassager() { return nbPassager; }
    public void setNbPassager(Integer nbPassager) { this.nbPassager = nbPassager; }

    public LocalDateTime getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(LocalDateTime dateHeureDepart) { this.dateHeureDepart = dateHeureDepart; }

    public LocalDateTime getDateHeureRetour() { return dateHeureRetour; }
    public void setDateHeureRetour(LocalDateTime dateHeureRetour) { this.dateHeureRetour = dateHeureRetour; }

    public Vehicule getVehicule() { return vehicule; }
    public void setVehicule(Vehicule vehicule) { this.vehicule = vehicule; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getNomHotel() { return nomHotel; }
    public void setNomHotel(String nomHotel) { this.nomHotel = nomHotel; }
}
