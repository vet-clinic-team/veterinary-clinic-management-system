
package com.efe.veterinaryclinic.followup.dto;

import com.efe.veterinaryclinic.followup.FollowUp;
import com.efe.veterinaryclinic.followup.FollowUpCompletionType;
import com.efe.veterinaryclinic.followup.FollowUpStatus;

import java.time.LocalDate;

public record FollowUpResponse(
        Long id,
        Long petId,
        String procedureType,
        LocalDate dueDate,
        FollowUpStatus status,
        FollowUpCompletionType completionType,
        String sourceType,
        Long sourceRecordId
) {
    public static FollowUpResponse from(FollowUp followUp) {
        return new FollowUpResponse(
                followUp.getId(),
                followUp.getPet().getId(),
                followUp.getProcedureType(),
                followUp.getDueDate(),
                followUp.getStatus(),
                followUp.getCompletionType(),
                followUp.getSourceType(),
                followUp.getSourceRecordId()
        );
    }
}

