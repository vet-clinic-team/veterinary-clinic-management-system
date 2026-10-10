
package com.efe.veterinaryclinic.followup;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "follow_up_rules")
@Getter
public class FollowUpRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String species;

    @Column(nullable = false, length = 100)
    private String procedureType;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean repeatAfterLastStep = false;

    @Column
    private Integer repeatIntervalValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IntervalUnit repeatIntervalUnit;

    @OneToMany(
            mappedBy = "rule",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("stepNumber ASC")
    private List<FollowUpRuleStep> steps = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected FollowUpRule() {
    }

    public FollowUpRule(
            String species,
            String procedureType,
            String name) {

        this.species = species;
        this.procedureType = procedureType;
        this.name = name;
        this.active = true;
        this.repeatAfterLastStep = false;
    }

    public void configureRepetition(
            int intervalValue,
            IntervalUnit intervalUnit) {

        if (intervalValue < 1) {
            throw new IllegalArgumentException(
                    "Repeat interval must be greater than zero");
        }

        if (intervalUnit == null) {
            throw new IllegalArgumentException(
                    "Repeat interval unit is required");
        }

        this.repeatAfterLastStep = true;
        this.repeatIntervalValue = intervalValue;
        this.repeatIntervalUnit = intervalUnit;
    }

    public void disableRepetition() {
        this.repeatAfterLastStep = false;
        this.repeatIntervalValue = null;
        this.repeatIntervalUnit = null;
    }

    public void addStep(
            int stepNumber,
            int intervalValue,
            IntervalUnit intervalUnit) {

        if (stepNumber < 1) {
            throw new IllegalArgumentException(
                    "Step number must be greater than zero");
        }

        if (intervalValue < 1) {
            throw new IllegalArgumentException(
                    "Interval value must be greater than zero");
        }

        if (intervalUnit == null) {
            throw new IllegalArgumentException(
                    "Interval unit is required");
        }

        boolean stepExists = steps.stream()
                .anyMatch(step -> step.getStepNumber() == stepNumber);

        if (stepExists) {
            throw new IllegalArgumentException(
                    "Step number already exists in this rule");
        }

        FollowUpRuleStep step = new FollowUpRuleStep(
                this,
                stepNumber,
                intervalValue,
                intervalUnit
        );

        steps.add(step);
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

