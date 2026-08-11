package com.efe.veterinaryclinic.pet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pet_weight_records")
@Getter
public class PetWeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false)
    private Double weightKg;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(length = 500)
    private String note;

    protected PetWeightRecord() {
    }

    public PetWeightRecord(Pet pet, Double weightKg, LocalDateTime recordedAt, String note) {
        this.pet = pet;
        this.weightKg = weightKg;
        this.recordedAt = recordedAt;
        this.note = note;
    }
}
