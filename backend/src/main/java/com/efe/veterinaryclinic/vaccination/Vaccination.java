package com.efe.veterinaryclinic.vaccination;

import com.efe.veterinaryclinic.pet.Pet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vaccinations")
@Getter
public class Vaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false, length = 100)
    private String vaccineType;

    @Column(nullable = false)
    private LocalDateTime administeredAt;

    @Column(length = 100)
    private String lotNumber;

    private LocalDate nextDueDate;

    @Column(length = 100)
    private String administeredBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Vaccination() {
    }

    public Vaccination(Pet pet, String vaccineType, LocalDateTime administeredAt, String lotNumber,
                        String administeredBy, LocalDate nextDueDate) {
        this.pet = pet;
        this.vaccineType = vaccineType;
        this.administeredAt = administeredAt;
        this.lotNumber = lotNumber;
        this.administeredBy = administeredBy;
        this.nextDueDate = nextDueDate;
    }

    public void update(Pet pet, String vaccineType, LocalDateTime administeredAt, String lotNumber,
                        String administeredBy, LocalDate nextDueDate) {
        this.pet = pet;
        this.vaccineType = vaccineType;
        this.administeredAt = administeredAt;
        this.lotNumber = lotNumber;
        this.administeredBy = administeredBy;
        this.nextDueDate = nextDueDate;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
