package com.s5.framework.dev.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.s5.framework.dev.models.TypeCarburant;
import com.s5.framework.dev.repositories.TypeCarburantRepository;

@Service
@Transactional
public class TypeCarburantService {

    private final TypeCarburantRepository typeCarburantRepository;

    @Autowired
    public TypeCarburantService(TypeCarburantRepository typeCarburantRepository) {
        this.typeCarburantRepository = typeCarburantRepository;
    }

    public List<TypeCarburant> findAll() {
        return typeCarburantRepository.findAll();
    }

    public Optional<TypeCarburant> findById(Long id) {
        return typeCarburantRepository.findById(id);
    }

    public TypeCarburant create(TypeCarburant typeCarburant) {
        return typeCarburantRepository.save(typeCarburant);
    }

    public void deleteById(Long id) {
        typeCarburantRepository.deleteById(id);
    }
}
