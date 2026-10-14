
package com.efe.veterinaryclinic.notification.dto;

import java.util.List;

public record NotificationResponse(
        List<UpcomingAppointmentNotification> upcomingAppointments,
        List<VaccinationDueTodayNotification> vaccinationsDueToday,
        List<NewRecordNotification> newRecords,
        List<PendingFollowUpNotification> pendingFollowUps
) {
}

