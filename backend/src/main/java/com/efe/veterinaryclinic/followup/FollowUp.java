
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.pet.Pet;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_ups")
@Getter
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false, length = 100)
    private String procedureType;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowUpStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_type", length = 20)
    private FollowUpCompletionType completionType;

    @Column(nullable = false, length = 50)
    private String sourceType;

    @Column(nullable = false)
    private Long sourceRecordId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected FollowUp() {
    }

    public FollowUp(
            Pet pet,
            String procedureType,
            LocalDate dueDate,
            String sourceType,
            Long sourceRecordId) {

        this.pet = pet;
        this.procedureType = procedureType;
        this.dueDate = dueDate;
        this.sourceType = sourceType;
        this.sourceRecordId = sourceRecordId;
        this.status = FollowUpStatus.PENDING;
        this.completionType = null;
    }

    public void updateDetails(
            Pet pet,
            String procedureType,
            LocalDate dueDate) {

        this.pet = pet;
        this.procedureType = procedureType;
        this.dueDate = dueDate;
    }

    // Complete a follow-up automatically.
    public void markCompleted() {
        markAutomaticallyCompleted();
    }

    public void markAutomaticallyCompleted() {
        this.status = FollowUpStatus.COMPLETED;
        this.completionType = FollowUpCompletionType.AUTOMATIC;
    }

    // Complete a follow-up manually.
    public void markManuallyCompleted() {
        this.status = FollowUpStatus.COMPLETED;
        this.completionType = FollowUpCompletionType.MANUAL;
    }

    // Reopen only automatically completed follow-ups.
    public void reopenAutomaticallyCompleted() {
        if (this.status == FollowUpStatus.COMPLETED
                && this.completionType == FollowUpCompletionType.AUTOMATIC) {

            this.status = FollowUpStatus.PENDING;
            this.completionType = null;
        }
    }

    // Reopen a cancelled follow-up when its source becomes active again.
    public void reopenCancelled() {
        if (this.status == FollowUpStatus.CANCELLED) {
            this.status = FollowUpStatus.PENDING;
            this.completionType = null;
        }
    }

    public void cancel() {
        this.status = FollowUpStatus.CANCELLED;
        this.completionType = null;
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

