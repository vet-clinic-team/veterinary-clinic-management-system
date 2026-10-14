
package com.efe.veterinaryclinic.treatment.dto;

import com.efe.veterinaryclinic.treatment.TreatmentPlan;
import com.efe.veterinaryclinic.treatment.TreatmentPlanStatus;

import java.time.LocalDateTime;

public record TreatmentPlanResponse(

        Long id,
        Long petId,
        String procedureType,
        TreatmentPlanStatus status,
        long completedApplications,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    // Convert a treatment plan into an API response.
    // The application count comes from completed visits.
    public static TreatmentPlanResponse from(
            TreatmentPlan plan,
            long completedApplications
    ) {
        return new TreatmentPlanResponse(
                plan.getId(),
                plan.getPet().getId(),
                plan.getProcedureType(),
                plan.getStatus(),
                completedApplications,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    // Preserve compatibility with existing controller calls.
    // This overload will be replaced after the controller is updated.
    public static TreatmentPlanResponse from(TreatmentPlan plan) {
        return from(plan, plan.getCompletedApplications());
    }
}

