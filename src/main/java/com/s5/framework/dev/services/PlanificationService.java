package com.s5.framework.dev.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.Planification;
import com.s5.framework.dev.models.PlanificationNonAssigne;
import com.s5.framework.dev.repositories.PlanificationNonAssigneRepository;
import com.s5.framework.dev.repositories.PlanificationRepository;

@Service
@Transactional
public class PlanificationService {

    private final PlanificationRepository planificationRepository;
    private final PlanificationNonAssigneRepository nonAssigneRepository;

    @Autowired
    public PlanificationService(PlanificationRepository planificationRepository,
                                PlanificationNonAssigneRepository nonAssigneRepository) {
        this.planificationRepository = planificationRepository;
        this.nonAssigneRepository = nonAssigneRepository;
    }

    public void clearForDate(LocalDate date) {
        planificationRepository.deleteByDate(date);
        nonAssigneRepository.deleteByDate(date);
    }

    public List<Planification> findByDate(LocalDate date) {
        return planificationRepository.findByDate(date);
    }

    public List<PlanificationNonAssigne> findNonAssigneByDate(LocalDate date) {
        return nonAssigneRepository.findByDate(date);
    }

    public void savePlanifications(List<Planification> planifications) {
        planificationRepository.saveAll(planifications);
    }

    public void saveNonAssignes(List<PlanificationNonAssigne> nonAssignes) {
        nonAssigneRepository.saveAll(nonAssignes);
    }
}
