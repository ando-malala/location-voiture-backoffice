package com.s5.framework.dev.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idhotel", nullable = false)
    private Hostel hotel;

    @Column(name = "idclient", nullable = false)
    private String idClient;

    @Column(name = "nbpassager", nullable = false)
    private Integer nbPassager;

    @Column(name = "dateheure", nullable = false)
    private LocalDate dateHeure;

    public Reservation() {
    }

    public Reservation(Long id, Hostel hotel, String idClient, Integer nbPassager, LocalDate dateHeure) {
        this.id = id;
        this.hotel = hotel;
        this.idClient = idClient;
        this.nbPassager = nbPassager;
        this.dateHeure = dateHeure;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hostel getHotel() {
        return hotel;
    }

    public void setHotel(Hostel hotel) {
        this.hotel = hotel;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) {
        this.idClient = idClient;
    }

    public Integer getNbPassager() {
        return nbPassager;
    }

    public void setNbPassager(Integer nbPassager) {
        this.nbPassager = nbPassager;
    }

    public LocalDate getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDate dateHeure) {
        this.dateHeure = dateHeure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Reservation))
            return false;
        Reservation that = (Reservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Reservation{" + "id=" + id + ", idClient='" + idClient + '\'' + '}';
    }
}
