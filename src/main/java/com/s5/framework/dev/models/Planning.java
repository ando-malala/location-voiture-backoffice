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

    public Planning() {}

    public Planning(Long idReservation, Integer nbPassager,
                    LocalDateTime dateHeureDepart, LocalDateTime dateHeureRetour,
                    Vehicule vehicule) {
        this.idReservation = idReservation;
        this.nbPassager = nbPassager;
        this.dateHeureDepart = dateHeureDepart;
        this.dateHeureRetour = dateHeureRetour;
        this.vehicule = vehicule;
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
}
