package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.Lieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LieuRepository extends JpaRepository<Lieu, Long> {

    Optional<Lieu> findByCode(String code);
}
