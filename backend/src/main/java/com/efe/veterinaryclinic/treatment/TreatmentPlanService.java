
package com.efe.veterinaryclinic.treatment;

import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.treatment.dto.TreatmentPlanResponse;
import com.efe.veterinaryclinic.visit.Visit;
import com.efe.veterinaryclinic.visit.VisitRepository;
import com.efe.veterinaryclinic.visit.VisitStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class TreatmentPlanService {

    private final TreatmentPlanRepository treatmentPlanRepository;
    private final PetRepository petRepository;

    private VisitRepository visitRepository;

    // Keep the existing constructor compatible with current tests.
    public TreatmentPlanService(
            TreatmentPlanRepository treatmentPlanRepository,
            PetRepository petRepository) {

        this.treatmentPlanRepository = treatmentPlanRepository;
        this.petRepository = petRepository;
    }

    // Inject the visit repository without changing the existing constructor.
    @Autowired
    public void setVisitRepository(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    // Create a separate treatment course for a pet.
    @Transactional
    public TreatmentPlan createPlan(Long petId, String procedureType) {

        if (procedureType == null || procedureType.isBlank()) {
            throw new IllegalArgumentException(
                    "Procedure type is required"
            );
        }

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pet not found: " + petId
                        )
                );

        TreatmentPlan plan = new TreatmentPlan(
                pet,
                procedureType.trim()
        );

        return treatmentPlanRepository.save(plan);
    }

    // Retrieve the treatment courses belonging to a pet.
    @Transactional(readOnly = true)
    public List<TreatmentPlan> getPlansByPet(Long petId) {
        return treatmentPlanRepository
                .findByPet_IdOrderByCreatedAtDesc(petId);
    }

    // Build a response using the actual number of completed visits.
    @Transactional(readOnly = true)
    public TreatmentPlanResponse toResponse(TreatmentPlan plan) {

        if (visitRepository == null) {
            throw new IllegalStateException(
                    "Visit repository is unavailable"
            );
        }

        long completedApplications = visitRepository
                .countByTreatmentPlan_IdAndStatus(
                        plan.getId(),
                        VisitStatus.COMPLETED
                );

        return TreatmentPlanResponse.from(
                plan,
                completedApplications
        );
    }

    // Link a visit to an explicitly selected treatment course.
    // This does not increment the application count.
    @Transactional
    public void assignVisitToPlan(
            Visit visit,
            Long treatmentPlanId) {

        TreatmentPlan plan = treatmentPlanRepository
                .findById(treatmentPlanId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Treatment plan not found: " + treatmentPlanId
                        )
                );

        if (plan.getStatus() != TreatmentPlanStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active treatment plans accept visits"
            );
        }

        if (!Objects.equals(
                plan.getPet().getId(),
                visit.getPet().getId())) {

            throw new IllegalArgumentException(
                    "Treatment plan belongs to another pet"
            );
        }

        if (visit.getProcedureType() == null
                || !plan.getProcedureType()
                        .equalsIgnoreCase(visit.getProcedureType())) {

            throw new IllegalArgumentException(
                    "Visit procedure does not match the treatment plan"
            );
        }

        visit.assignTreatmentPlan(plan);
    }

    // Count completed visits linked to the same treatment course.
    // A distinct visit is counted only once.
    @Transactional(readOnly = true)
    public long countPreviousApplications(
            Visit visit,
            List<Visit> planVisits) {

        if (visit.getTreatmentPlan() == null) {
            return 0;
        }

        Long planId = visit.getTreatmentPlan().getId();

        return planVisits.stream()
                .filter(other ->
                        other.getTreatmentPlan() != null
                        && Objects.equals(
                                other.getTreatmentPlan().getId(),
                                planId
                        )
                        && other.getStatus() == VisitStatus.COMPLETED
                        && other.getScheduledAt()
                                .isBefore(visit.getScheduledAt())
                )
                .map(Visit::getId)
                .distinct()
                .count();
    }
}


