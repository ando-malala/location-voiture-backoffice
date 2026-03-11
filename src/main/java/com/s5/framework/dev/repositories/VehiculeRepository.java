package com.s5.framework.dev.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.s5.framework.dev.models.Vehicule;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {
}
