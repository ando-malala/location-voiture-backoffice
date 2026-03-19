package com.s5.framework.dev.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.s5.framework.dev.models.PlanificationNonAssigne;

public interface PlanificationNonAssigneRepository extends JpaRepository<PlanificationNonAssigne, Long> {

    List<PlanificationNonAssigne> findByDate(LocalDate date);

    void deleteByDate(LocalDate date);
}
