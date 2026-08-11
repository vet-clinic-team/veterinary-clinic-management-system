package com.efe.veterinaryclinic.pet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PetWeightRecordRequest(
        @NotNull(message = "must not be null")
        @Positive(message = "must be positive")
        Double weightKg,

        @NotNull(message = "must not be null")
        LocalDateTime recordedAt,

        @Size(max = 500, message = "must be at most 500 characters")
        String note
) {
}
