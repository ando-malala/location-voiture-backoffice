package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT v FROM Vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH v.type t " +
           "LEFT JOIN FETCH v.vehicleStatus s")
    java.util.List<Vehicle> findAllWithAssociations();
} 
