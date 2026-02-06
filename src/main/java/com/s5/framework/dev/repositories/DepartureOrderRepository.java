package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.DepartureOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartureOrderRepository extends JpaRepository<DepartureOrder, Long> {
    
    List<DepartureOrder> findByReservationId(Long reservationId);
    
    List<DepartureOrder> findByVehicleId(Long vehicleId);
}
