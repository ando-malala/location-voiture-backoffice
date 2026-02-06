package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    
    @Query("SELECT DISTINCT m FROM Maintenance m " +
           "LEFT JOIN FETCH m.vehicle v")
    List<Maintenance> findAllWithAssociations();
}
