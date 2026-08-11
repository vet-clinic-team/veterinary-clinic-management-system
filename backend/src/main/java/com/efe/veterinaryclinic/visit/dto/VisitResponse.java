package com.efe.veterinaryclinic.visit.dto;

import com.efe.veterinaryclinic.visit.AllergyWarningChecker;
import com.efe.veterinaryclinic.visit.Visit;
import com.efe.veterinaryclinic.visit.VisitStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record VisitResponse(
        Long id,
        Long petId,
        Long vetId,
        LocalDateTime scheduledAt,
        VisitStatus status,
        String chiefComplaint,
        String diagnosis,
        String treatmentNotes,
        LocalDate followUpDate,
        List<String> warnings,
        LocalDateTime reminderSentAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static VisitResponse from(Visit visit) {
        return new VisitResponse(
                visit.getId(),
                visit.getPet().getId(),
                visit.getVet().getId(),
                visit.getScheduledAt(),
                visit.getStatus(),
                visit.getChiefComplaint(),
                visit.getDiagnosis(),
                visit.getTreatmentNotes(),
                visit.getFollowUpDate(),
                AllergyWarningChecker.check(visit.getTreatmentNotes(), visit.getPet().getAllergies()),
                visit.getReminderSentAt(),
                visit.getCreatedAt(),
                visit.getUpdatedAt()
        );
    }
}
