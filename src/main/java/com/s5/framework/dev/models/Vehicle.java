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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String vin;

    @Column(unique = true, nullable = false)
    private String licensePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private VehicleModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    private VehicleType type;

    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    private String color;

    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private VehicleStatus vehicleStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Vehicle() {
    }

    public Vehicle(Long id, String vin, String licensePlate, VehicleModel model, VehicleType type, Location location,
            String color, Integer year, VehicleStatus vehicleStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.vin = vin;
        this.licensePlate = licensePlate;
        this.model = model;
        this.type = type;
        this.location = location;
        this.color = color;
        this.year = year;
        this.vehicleStatus = vehicleStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public VehicleModel getModel() {
        return model;
    }

    public void setModel(VehicleModel model) {
        this.model = model;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Vehicle))
            return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Vehicle{" + "id=" + id + ", licensePlate='" + licensePlate + '\'' + '}';
    }
}
