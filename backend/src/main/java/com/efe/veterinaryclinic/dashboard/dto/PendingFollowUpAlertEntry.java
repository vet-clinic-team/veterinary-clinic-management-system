
package com.efe.veterinaryclinic.dashboard.dto;

import com.efe.veterinaryclinic.followup.FollowUpStatus;
import java.time.LocalDate;

public record PendingFollowUpAlertEntry(
        Long followUpId,
        Long petId,
        String petName,
        String procedureType,
        LocalDate dueDate,
        FollowUpStatus status
) {
}

