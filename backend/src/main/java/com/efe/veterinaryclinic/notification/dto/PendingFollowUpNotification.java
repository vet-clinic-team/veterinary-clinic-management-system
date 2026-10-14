
package com.efe.veterinaryclinic.notification.dto;

import java.time.LocalDate;

public record PendingFollowUpNotification(
        Long followUpId,
        Long petId,
        String petName,
        String procedureType,
        LocalDate dueDate
) {
}

