package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleStatusRepository extends JpaRepository<VehicleStatus, Long> {
    Optional<VehicleStatus> findByCode(String code);
}
