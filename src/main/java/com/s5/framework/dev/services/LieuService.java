package com.s5.framework.dev.services;

import com.s5.framework.dev.models.Lieu;
import com.s5.framework.dev.repositories.LieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LieuService {

    private final LieuRepository lieuRepository;

    @Autowired
    public LieuService(LieuRepository lieuRepository) {
        this.lieuRepository = lieuRepository;
    }

    public List<Lieu> findAll() {
        return lieuRepository.findAll();
    }

    public Optional<Lieu> findById(Long id) {
        return lieuRepository.findById(id);
    }

    public Lieu create(Lieu lieu) {
        return lieuRepository.save(lieu);
    }

    public void deleteById(Long id) {
        lieuRepository.deleteById(id);
    }
}
