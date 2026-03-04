package com.s5.framework.dev.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.s5.framework.dev.models.Distance;

@Repository
public interface DistanceRepository extends JpaRepository<Distance, Long> {

    /**
     * Trouve la distance entre deux hôtels par leurs IDs.
     */
    Optional<Distance> findByHotelDepartIdAndHotelArriveeId(Long hotelDepartId, Long hotelArriveeId);
}
