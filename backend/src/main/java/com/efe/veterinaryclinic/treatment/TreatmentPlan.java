package com.efe.veterinaryclinic.treatment;

import com.efe.veterinaryclinic.pet.Pet;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "treatment_plans")
@Getter
public class TreatmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false, length = 100)
    private String procedureType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TreatmentPlanStatus status;

    @Column(nullable = false)
    private int completedApplications = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected TreatmentPlan() {
    }

    public TreatmentPlan(Pet pet, String procedureType) {
        this.pet = pet;
        this.procedureType = procedureType;
        this.status = TreatmentPlanStatus.ACTIVE;
        this.completedApplications = 0;
    }

    public void completeApplication() {
        if (status != TreatmentPlanStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active treatment plans can record applications"
            );
        }

        completedApplications++;
    }

    public void markCompleted() {
        this.status = TreatmentPlanStatus.COMPLETED;
    }

    public void cancel() {
        this.status = TreatmentPlanStatus.CANCELLED;
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
