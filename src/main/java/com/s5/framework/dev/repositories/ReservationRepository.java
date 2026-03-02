package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Recherche les réservations dont dateHeure est compris entre start (inclus) et end (inclus).
     * Utilisé pour filtrer par une journée entière.
     */
    List<Reservation> findByDateHeureBetween(LocalDateTime start, LocalDateTime end);
}
