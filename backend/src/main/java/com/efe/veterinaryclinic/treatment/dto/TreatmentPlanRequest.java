
package com.efe.veterinaryclinic.treatment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TreatmentPlanRequest(

        @NotNull(message = "Pet ID is required")
        @Positive(message = "Pet ID must be positive")
        Long petId,

        @NotBlank(message = "Procedure type is required")
        @Size(max = 100, message = "must be at most 100 characters")
        String procedureType

) {
}

