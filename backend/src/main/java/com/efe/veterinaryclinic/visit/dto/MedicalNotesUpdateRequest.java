package com.efe.veterinaryclinic.visit.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MedicalNotesUpdateRequest(
        @Size(max = 2000, message = "must be at most 2000 characters")
        String diagnosis,

        @Size(max = 4000, message = "must be at most 4000 characters")
        String treatmentNotes,

        LocalDate followUpDate
) {
}
