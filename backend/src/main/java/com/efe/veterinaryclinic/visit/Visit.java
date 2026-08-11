package com.efe.veterinaryclinic.visit;

import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.vet.Vet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "visits")
@Getter
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", nullable = false)
    private Vet vet;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitStatus status;

    @Column(nullable = false, length = 500)
    private String chiefComplaint;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 4000)
    private String treatmentNotes;

    private LocalDate followUpDate;

    private LocalDateTime reminderSentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Visit() {
    }

    public Visit(Pet pet, Vet vet, LocalDateTime scheduledAt, String chiefComplaint) {
        this.pet = pet;
        this.vet = vet;
        this.scheduledAt = scheduledAt;
        this.chiefComplaint = chiefComplaint;
        this.status = VisitStatus.SCHEDULED;
    }

    public void update(Pet pet, Vet vet, LocalDateTime scheduledAt, String chiefComplaint) {
        this.pet = pet;
        this.vet = vet;
        this.scheduledAt = scheduledAt;
        this.chiefComplaint = chiefComplaint;
    }

    public void updateStatus(VisitStatus status) {
        this.status = status;
    }

    public void updateMedicalNotes(String diagnosis, String treatmentNotes, LocalDate followUpDate) {
        this.diagnosis = diagnosis;
        this.treatmentNotes = treatmentNotes;
        this.followUpDate = followUpDate;
    }

    public void markReminderSent() {
        this.reminderSentAt = LocalDateTime.now();
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
