package com.s5.framework.dev.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.Planning;
import com.s5.framework.dev.repositories.PlanningRepository;

@Service
@Transactional
public class PlanningService {

    private final PlanningRepository planningRepository;

    @Autowired
    public PlanningService(PlanningRepository planningRepository) {
        this.planningRepository = planningRepository;
    }

    /**
     * Retourne tous les plannings triés par date de départ.
     */
    public List<Planning> findAll() {
        return planningRepository.findAllByOrderByDateHeureDepartAsc();
    }

    public Optional<Planning> findById(Long id) {
        return planningRepository.findById(id);
    }

    /**
     * Recherche tous les plannings pour une date donnée (journée entière).
     */
    public List<Planning> findByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return planningRepository.findByDateHeureDepartBetweenOrderByDateHeureDepartAsc(start, end);
    }

    /**
     * Recherche les plannings d'un véhicule spécifique.
     */
    public List<Planning> findByVehiculeId(Long vehiculeId) {
        return planningRepository.findByVehiculeIdOrderByDateHeureDepartAsc(vehiculeId);
    }

    public Planning create(Planning planning) {
        return planningRepository.save(planning);
    }

    public Planning update(Planning planning) {
        return planningRepository.save(planning);
    }

    public void deleteById(Long id) {
        planningRepository.deleteById(id);
    }
}
