package com.s5.framework.dev.services;

import java.util.List;
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

    public List<Distance> findAll() {
        return distanceRepository.findAll();
    }

    public Optional<Distance> findById(Long id) {
        return distanceRepository.findById(id);
    }

    public Distance create(Distance distance) {
        return distanceRepository.save(distance);
    }

    public Distance update(Distance distance) {
        return distanceRepository.save(distance);
    }

    public void deleteById(Long id) {
        distanceRepository.deleteById(id);
    }

    /**
     * Retourne la distance (en km) entre deux hôtels.
     */
    public Optional<Distance> findByHotels(Long hotelDepartId, Long hotelArriveeId) {
        return distanceRepository.findByHotelDepartIdAndHotelArriveeId(hotelDepartId, hotelArriveeId);
    }

    /**
     * Retourne la distance en km entre deux hôtels, ou lève une exception si introuvable.
     */
    public double getDistanceKm(Long hotelDepartId, Long hotelArriveeId) {
        return distanceRepository.findByHotelDepartIdAndHotelArriveeId(hotelDepartId, hotelArriveeId)
                .map(Distance::getDistanceKm)
                .orElseThrow(() -> new RuntimeException(
                        "Distance introuvable entre hotel " + hotelDepartId + " et hotel " + hotelArriveeId));
    }
}
