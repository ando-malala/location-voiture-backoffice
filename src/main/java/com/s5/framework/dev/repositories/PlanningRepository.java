package com.s5.framework.dev.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
