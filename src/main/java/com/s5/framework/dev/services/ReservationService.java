package com.s5.framework.dev.services;

import com.s5.framework.dev.models.Reservation;
import com.s5.framework.dev.repositories.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }

    public Reservation create(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    public Reservation update(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    public void delete(Reservation reservation) {
        reservationRepository.delete(reservation);
    }

    public void deleteById(Long id) {
        reservationRepository.deleteById(id);
    }
}
