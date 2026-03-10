package com.s5.framework.dev.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de simulation de planification (non persisté en base).
 * Représente UN TRAJET complet d'un véhicule (simple ou combiné).
 *
 * <p>Un trajet simple contient une seule {@link ResInfo}.
 * Un trajet combiné en contient plusieurs (même véhicule, même créneau),
 * ordonnées du plus proche au plus éloigné de l'aéroport.</p>
 */
public class Planning {

    // ------------------------------------------------------------------ //
    //  Inner class : détail d'une réservation dans le trajet               //
    // ------------------------------------------------------------------ //

    public static class ResInfo {
        private final Long    idReservation;
        private final Integer nbPassager;
        private final String  nomHotel;
        private final Double  distanceKm;

        public ResInfo(Long idReservation, Integer nbPassager,
                       String nomHotel, Double distanceKm) {
            this.idReservation = idReservation;
            this.nbPassager    = nbPassager;
            this.nomHotel      = nomHotel;
            this.distanceKm    = distanceKm;
        }

        public Long    getIdReservation() { return idReservation; }
        public Integer getNbPassager()    { return nbPassager; }
        public String  getNomHotel()      { return nomHotel; }
        public Double  getDistanceKm()    { return distanceKm; }
    }

    // ------------------------------------------------------------------ //
    //  Champs du trajet                                                    //
    // ------------------------------------------------------------------ //

    /** Réservations couvertes, dans l'ordre de visite (plus proche → plus éloigné). */
    private List<ResInfo> reservations = new ArrayList<>();

    private LocalDateTime dateHeureDepart;
    private LocalDateTime dateHeureRetour;
    private Vehicule      vehicule;

    /** {@code true} si plusieurs réservations partagent ce trajet. */
    private boolean combinedTrip = false;

    /** Noms des hôtels dans l'ordre de visite (pour l'affichage de la route). */
    private List<String> routeHotels = new ArrayList<>();

    // ------------------------------------------------------------------ //
    //  Constructeurs                                                       //
    // ------------------------------------------------------------------ //

    public Planning() {}

    public Planning(List<ResInfo> reservations,
                    LocalDateTime dateHeureDepart, LocalDateTime dateHeureRetour,
                    Vehicule vehicule,
                    boolean combinedTrip, List<String> routeHotels) {
        this.reservations    = reservations  != null ? reservations  : new ArrayList<>();
        this.dateHeureDepart = dateHeureDepart;
        this.dateHeureRetour = dateHeureRetour;
        this.vehicule        = vehicule;
        this.combinedTrip    = combinedTrip;
        this.routeHotels     = routeHotels   != null ? routeHotels   : new ArrayList<>();
    }

    // ------------------------------------------------------------------ //
    //  Getters de commodité (première réservation)                         //
    // ------------------------------------------------------------------ //

    public Long    getIdReservation() { return reservations.isEmpty() ? null : reservations.get(0).getIdReservation(); }
    public Integer getNbPassager()    { return reservations.isEmpty() ? null : reservations.get(0).getNbPassager(); }
    public String  getNomHotel()      { return reservations.isEmpty() ? ""   : reservations.get(0).getNomHotel(); }
    public Double  getDistanceKm()    { return reservations.isEmpty() ? null : reservations.get(0).getDistanceKm(); }

    /** Somme des passagers de toutes les réservations du trajet. */
    public int getTotalPassagers() {
        return reservations.stream().mapToInt(ResInfo::getNbPassager).sum();
    }

    // ------------------------------------------------------------------ //
    //  Getters / Setters                                                   //
    // ------------------------------------------------------------------ //

    public List<ResInfo>  getReservations()    { return reservations; }
    public void setReservations(List<ResInfo> r) { this.reservations = r; }

    public LocalDateTime getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(LocalDateTime v) { this.dateHeureDepart = v; }

    public LocalDateTime getDateHeureRetour() { return dateHeureRetour; }
    public void setDateHeureRetour(LocalDateTime v) { this.dateHeureRetour = v; }

    public Vehicule getVehicule() { return vehicule; }
    public void setVehicule(Vehicule v) { this.vehicule = v; }

    public boolean isCombinedTrip() { return combinedTrip; }
    public void setCombinedTrip(boolean v) { this.combinedTrip = v; }

    public List<String> getRouteHotels() { return routeHotels; }
    public void setRouteHotels(List<String> v) { this.routeHotels = v; }
}
