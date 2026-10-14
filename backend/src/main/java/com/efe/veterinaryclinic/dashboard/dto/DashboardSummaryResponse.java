
package com.efe.veterinaryclinic.dashboard.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long todayAppointments,
        long activePatients,
        long pendingVaccinations,
        long unpaidInvoices,
        List<MonthlyRevenueEntry> monthlyRevenue,
        List<RevenueCategoryEntry> revenueByCategory,
        List<AppointmentTrendEntry> appointmentTrend30Days,
        List<CumulativeAppointmentEntry> cumulativeAppointmentsYtd,
        List<VetAppointmentCountEntry> appointmentsByVet,
        List<TodayScheduleEntry> todaySchedule,
        List<VaccinationAlertEntry> upcomingVaccinationAlerts,
        List<FollowUpAlertEntry> overdueFollowUpAlerts,
        List<PendingFollowUpAlertEntry> pendingFollowUpAlerts
) {
}
