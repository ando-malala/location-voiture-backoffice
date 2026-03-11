package com.s5.framework.dev.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.s5.framework.dev.models.Parametre;

@Repository
public interface ParametreRepository extends JpaRepository<Parametre, Long> {

    /**
     * Trouve un paramètre par son libellé (ex: "Vitesse moyenne", "Temps d attente").
     */
    Optional<Parametre> findByLibelle(String libelle);
}
