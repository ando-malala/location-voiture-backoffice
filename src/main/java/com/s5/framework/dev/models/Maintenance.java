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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "maintenance")
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "description")
    private String description;

    @Column(name = "cost")
    private java.math.BigDecimal cost;

    @Column(name = "maintenance_date")
    private LocalDate maintenanceDate;

    @Column(name = "mileage")
    private Integer mileage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Maintenance() {
    }

    public Maintenance(Long id, Vehicle vehicle, String description, java.math.BigDecimal cost,
            LocalDate maintenanceDate, Integer mileage, LocalDateTime createdAt) {
        this.id = id;
        this.vehicle = vehicle;
        this.description = description;
        this.cost = cost;
        this.maintenanceDate = maintenanceDate;
        this.mileage = mileage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.math.BigDecimal getCost() {
        return cost;
    }

    public void setCost(java.math.BigDecimal cost) {
        this.cost = cost;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Maintenance))
            return false;
        Maintenance that = (Maintenance) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Maintenance{" + "id=" + id + '}';
    }
}
