package com.s5.framework.dev.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.Vehicule;
import com.s5.framework.dev.repositories.VehiculeRepository;

@Service
@Transactional
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;

    @Autowired
    public VehiculeService(VehiculeRepository vehiculeRepository) {
        this.vehiculeRepository = vehiculeRepository;
    }

    public List<Vehicule> findAll() {
        return vehiculeRepository.findAll();
    }

    public Optional<Vehicule> findById(Long id) {
        return vehiculeRepository.findById(id);
    }

    public Vehicule create(Vehicule vehicule) {
        return vehiculeRepository.save(vehicule);
    }

    public Vehicule update(Vehicule vehicule) {
        return vehiculeRepository.save(vehicule);
    }

    public void deleteById(Long id) {
        vehiculeRepository.deleteById(id);
    }
}
