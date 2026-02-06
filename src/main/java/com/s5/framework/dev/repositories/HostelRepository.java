package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HostelRepository extends JpaRepository<Hostel, Long> {
    
    Optional<Hostel> findByName(String name);
}
