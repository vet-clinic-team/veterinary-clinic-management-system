package com.efe.veterinaryclinic.vaccination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record VaccinationRequest(
        @NotNull(message = "must not be null")
        Long petId,

        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        String vaccineType,

        @NotNull(message = "must not be null")
        LocalDateTime administeredAt,

        @Size(max = 100, message = "must be at most 100 characters")
        String lotNumber,

        @Size(max = 100, message = "must be at most 100 characters")
        String administeredBy
) {
}
