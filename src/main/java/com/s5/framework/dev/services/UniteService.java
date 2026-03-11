package com.s5.framework.dev.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.Unite;
import com.s5.framework.dev.repositories.UniteRepository;

@Service
@Transactional
public class UniteService {

    private final UniteRepository uniteRepository;

    @Autowired
    public UniteService(UniteRepository uniteRepository) {
        this.uniteRepository = uniteRepository;
    }

    public List<Unite> findAll() {
        return uniteRepository.findAll();
    }

    public Optional<Unite> findById(Long id) {
        return uniteRepository.findById(id);
    }

    public Unite create(Unite unite) {
        return uniteRepository.save(unite);
    }

    public Unite update(Unite unite) {
        return uniteRepository.save(unite);
    }

    public void deleteById(Long id) {
        uniteRepository.deleteById(id);
    }
}
