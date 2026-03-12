package com.s5.framework.dev.services;

import java.util.List;
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

    public List<Parametre> findAll() {
        return parametreRepository.findAll();
    }

    public Optional<Parametre> findById(Long id) {
        return parametreRepository.findById(id);
    }

    public Parametre create(Parametre parametre) {
        return parametreRepository.save(parametre);
    }

    public Parametre update(Parametre parametre) {
        return parametreRepository.save(parametre);
    }

    public void deleteById(Long id) {
        parametreRepository.deleteById(id);
    }

    public Optional<Parametre> findByLibelle(String libelle) {
        return parametreRepository.findByLibelle(libelle);
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
}
