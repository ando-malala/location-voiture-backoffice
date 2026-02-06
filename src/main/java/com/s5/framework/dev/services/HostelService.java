package com.s5.framework.dev.services;

import com.s5.framework.dev.models.Hostel;
import com.s5.framework.dev.repositories.HostelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HostelService {

    private final HostelRepository hostelRepository;

    @Autowired
    public HostelService(HostelRepository hostelRepository) {
        this.hostelRepository = hostelRepository;
    }

    public List<Hostel> findAll() {
        return hostelRepository.findAll();
    }

    public Optional<Hostel> findById(Long id) {
        return hostelRepository.findById(id);
    }

    public Optional<Hostel> findByNom(String nom) {
        return hostelRepository.findByNom(nom);
    }

    public Hostel create(Hostel hostel) {
        return hostelRepository.save(hostel);
    }

    public Hostel update(Hostel hostel) {
        return hostelRepository.save(hostel);
    }

    public void delete(Hostel hostel) {
        hostelRepository.delete(hostel);
    }

    public void deleteById(Long id) {
        hostelRepository.deleteById(id);
    }
}
