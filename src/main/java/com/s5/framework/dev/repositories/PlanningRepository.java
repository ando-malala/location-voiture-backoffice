package com.s5.framework.dev.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.s5.framework.dev.models.Planning;

@Repository
public interface PlanningRepository extends JpaRepository<Planning, Long> {

    /**
     * Recherche les plannings dont la date de départ est comprise entre start et end.
     * Utilisé pour filtrer par une journée entière.
     */
    List<Planning> findByDateHeureDepartBetweenOrderByDateHeureDepartAsc(LocalDateTime start, LocalDateTime end);

    /**
     * Recherche les plannings par véhicule.
     */
    List<Planning> findByVehiculeIdOrderByDateHeureDepartAsc(Long vehiculeId);

    /**
     * Tous les plannings triés par date de départ.
     */
    List<Planning> findAllByOrderByDateHeureDepartAsc();

    /**
     * Vérifie si un véhicule a un planning qui chevauche la période [depart, retour].
     */
    @Query("SELECT p FROM Planning p WHERE p.vehicule.id = :vehiculeId " +
           "AND p.dateHeureDepart < :retour AND p.dateHeureRetour > :depart")
    List<Planning> findOverlapping(@Param("vehiculeId") Long vehiculeId,
                                   @Param("depart") LocalDateTime depart,
                                   @Param("retour") LocalDateTime retour);

    /**
     * Vérifie si une réservation a déjà un planning assigné.
     */
    List<Planning> findByReservationId(Long reservationId);
}
