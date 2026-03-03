package com.s5.framework.dev.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.Parametre;
import com.s5.framework.dev.repositories.ParametreRepository;

@Service
@Transactional
public class ParametreService {

    private final ParametreRepository parametreRepository;

    @Autowired
    public ParametreService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    /**
     * Récupère la vitesse moyenne (en km/h).
     */
    public int getVitesseMoyenne() {
        return parametreRepository.findByLibelle("Vitesse moyenne")
                .map(Parametre::getValeur)
                .orElseThrow(() -> new RuntimeException("Paramètre 'Vitesse moyenne' non trouvé en base"));
    }

    /**
     * Récupère le temps d'attente à l'hôtel (en minutes).
     */
    public int getTempsAttente() {
        return parametreRepository.findByLibelle("Temps d attente")
                .map(Parametre::getValeur)
                .orElseThrow(() -> new RuntimeException("Paramètre 'Temps d attente' non trouvé en base"));
    }

    public Optional<Parametre> findByLibelle(String libelle) {
        return parametreRepository.findByLibelle(libelle);
    }
}
