package com.s5.framework.dev.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.s5.framework.dev.models.TypeCarburant;

@Repository
public interface TypeCarburantRepository extends JpaRepository<TypeCarburant, Long> {
}
