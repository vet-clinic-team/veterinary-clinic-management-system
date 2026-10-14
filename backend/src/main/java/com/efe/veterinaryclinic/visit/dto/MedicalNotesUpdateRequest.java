
package com.efe.veterinaryclinic.visit.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MedicalNotesUpdateRequest(

        @Size(max = 2000, message = "must be at most 2000 characters")
        String diagnosis,

        @Size(max = 4000, message = "must be at most 4000 characters")
        String treatmentNotes,

        LocalDate followUpDate,

        // Optional procedure type for automatic follow-up rules.
        @Size(max = 100, message = "must be at most 100 characters")
        String procedureType,

        // False explicitly disables automatic follow-up calculation.
        // Null preserves the default automatic behavior.
        Boolean useAutomaticFollowUp,

        // Optional ID of the treatment course associated with this visit.
        Long treatmentPlanId

) {

    // Preserve compatibility with existing three-argument constructors.
    public MedicalNotesUpdateRequest(
            String diagnosis,
            String treatmentNotes,
            LocalDate followUpDate
    ) {
        this(diagnosis, treatmentNotes, followUpDate, null, null, null);
    }

    // Preserve compatibility with existing four-argument constructors.
    public MedicalNotesUpdateRequest(
            String diagnosis,
            String treatmentNotes,
            LocalDate followUpDate,
            String procedureType
    ) {
        this(diagnosis, treatmentNotes, followUpDate, procedureType, null, null);
    }

    // Preserve compatibility with existing five-argument constructors.
    public MedicalNotesUpdateRequest(
            String diagnosis,
            String treatmentNotes,
            LocalDate followUpDate,
            String procedureType,
            Boolean useAutomaticFollowUp
    ) {
        this(
                diagnosis,
                treatmentNotes,
                followUpDate,
                procedureType,
                useAutomaticFollowUp,
                null
        );
    }
}




