package com.s5.framework.dev.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.s5.framework.dev.models.Planification;

public interface PlanificationRepository extends JpaRepository<Planification, Long> {

    List<Planification> findByDate(LocalDate date);

    void deleteByDate(LocalDate date);
}
