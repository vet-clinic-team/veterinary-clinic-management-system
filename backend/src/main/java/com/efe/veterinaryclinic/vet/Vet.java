package com.efe.veterinaryclinic.vet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vets")
@Getter
public class Vet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String specialty;

    @Column(nullable = false, unique = true, length = 50)
    private String licenseNo;

    @Column(nullable = false, length = 100)
    private String workHours;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Vet() {
    }

    public Vet(String name, String specialty, String licenseNo, String workHours) {
        this.name = name;
        this.specialty = specialty;
        this.licenseNo = licenseNo;
        this.workHours = workHours;
        this.active = true;
    }

    public void update(String name, String specialty, String licenseNo, String workHours) {
        this.name = name;
        this.specialty = specialty;
        this.licenseNo = licenseNo;
        this.workHours = workHours;
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
