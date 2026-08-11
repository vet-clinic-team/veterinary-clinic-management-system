package com.efe.veterinaryclinic.visit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record VisitRequest(
        @NotNull(message = "must not be null")
        Long petId,

        @NotNull(message = "must not be null")
        Long vetId,

        @NotNull(message = "must not be null")
        LocalDateTime scheduledAt,

        @NotBlank(message = "must not be blank")
        @Size(max = 500, message = "must be at most 500 characters")
        String chiefComplaint
) {
}
