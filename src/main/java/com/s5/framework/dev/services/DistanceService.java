package com.s5.framework.dev.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.Distance;
import com.s5.framework.dev.repositories.DistanceRepository;

@Service
@Transactional
public class DistanceService {

    private final DistanceRepository distanceRepository;

    @Autowired
    public DistanceService(DistanceRepository distanceRepository) {
        this.distanceRepository = distanceRepository;
    }

    /**
     * Retourne la distance (en km) entre deux lieux.
     */
    public Optional<Distance> findByLieux(Long lieuDepartId, Long lieuArriveeId) {
        return distanceRepository.findByLieuDepartIdAndLieuArriveeId(lieuDepartId, lieuArriveeId);
    }

    /**
     * Retourne la distance en km entre deux lieux, ou lève une exception si introuvable.
     */
    public double getDistanceKm(Long lieuDepartId, Long lieuArriveeId) {
        return distanceRepository.findByLieuDepartIdAndLieuArriveeId(lieuDepartId, lieuArriveeId)
                .map(Distance::getDistanceKm)
                .orElseThrow(() -> new RuntimeException(
                        "Distance introuvable entre lieu " + lieuDepartId + " et lieu " + lieuArriveeId));
    }
}
