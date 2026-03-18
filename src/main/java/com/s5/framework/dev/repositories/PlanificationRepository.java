package com.s5.framework.dev.repositories;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.s5.framework.dev.models.Planification;

@Repository
public interface PlanificationRepository extends JpaRepository<Planification, Long> {

    void deleteByDateJour(LocalDate dateJour);

    long countByDateJourAndVehicule_Id(LocalDate dateJour, Long vehiculeId);
}
