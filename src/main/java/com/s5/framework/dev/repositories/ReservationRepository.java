package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    @Query("SELECT DISTINCT r FROM Reservation r " +
           "LEFT JOIN FETCH r.client c " +
           "LEFT JOIN FETCH r.vehicle v " +
           "LEFT JOIN FETCH r.reservationStatus s")
    List<Reservation> findAllWithAssociations();
}
