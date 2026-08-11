package com.efe.veterinaryclinic.pet;

import com.efe.veterinaryclinic.owner.Owner;
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
@Table(name = "pets")
@Getter
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String species;

    @Column(length = 100)
    private String breed;

    @Column(length = 200)
    private String speciesNote;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 20)
    private String sex;

    @Column(nullable = false)
    private Double weightKg;

    @Column(length = 500)
    private String allergies;

    @Column(length = 500)
    private String chronicConditions;

    @Column(nullable = false)
    private boolean archived;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Pet() {
    }

    public Pet(Owner owner, String name, String species, String breed, String speciesNote,
               LocalDate birthDate, String sex, Double weightKg, String allergies, String chronicConditions) {
        this.owner = owner;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.speciesNote = speciesNote;
        this.birthDate = birthDate;
        this.sex = sex;
        this.weightKg = weightKg;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.archived = false;
    }

    public void update(Owner owner, String name, String species, String breed, String speciesNote,
                        LocalDate birthDate, String sex, Double weightKg, String allergies, String chronicConditions) {
        this.owner = owner;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.speciesNote = speciesNote;
        this.birthDate = birthDate;
        this.sex = sex;
        this.weightKg = weightKg;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
    }

    public void archive() {
        this.archived = true;
    }

    public void activate() {
        this.archived = false;
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
