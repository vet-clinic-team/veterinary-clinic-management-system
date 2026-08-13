package com.efe.veterinaryclinic.dashboard;

import com.efe.veterinaryclinic.dashboard.dto.AppointmentTrendEntry;
import com.efe.veterinaryclinic.dashboard.dto.CumulativeAppointmentEntry;
import com.efe.veterinaryclinic.dashboard.dto.DashboardSummaryResponse;
import com.efe.veterinaryclinic.dashboard.dto.FollowUpAlertEntry;
import com.efe.veterinaryclinic.dashboard.dto.MonthlyRevenueEntry;
import com.efe.veterinaryclinic.dashboard.dto.RevenueCategoryEntry;
import com.efe.veterinaryclinic.dashboard.dto.TodayScheduleEntry;
import com.efe.veterinaryclinic.dashboard.dto.VaccinationAlertEntry;
import com.efe.veterinaryclinic.dashboard.dto.VetAppointmentCountEntry;
import com.efe.veterinaryclinic.invoice.Invoice;
import com.efe.veterinaryclinic.invoice.InvoiceItem;
import com.efe.veterinaryclinic.invoice.InvoiceItemCategory;
import com.efe.veterinaryclinic.invoice.InvoiceItemRepository;
import com.efe.veterinaryclinic.invoice.InvoiceRepository;
import com.efe.veterinaryclinic.invoice.InvoiceStatus;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.vaccination.VaccinationRepository;
import com.efe.veterinaryclinic.vet.VetRepository;
import com.efe.veterinaryclinic.visit.Visit;
import com.efe.veterinaryclinic.visit.VisitRepository;
import com.efe.veterinaryclinic.visit.VisitStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int REVENUE_MONTHS_WINDOW = 12;
    private static final int TREND_DAYS_WINDOW = 30;
    private static final int VACCINATION_ALERT_WINDOW_DAYS = 30;
    private static final int CURRENCY_SCALE = 2;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final VisitRepository visitRepository;
    private final PetRepository petRepository;
    private final VaccinationRepository vaccinationRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final VetRepository vetRepository;

    public DashboardService(VisitRepository visitRepository, PetRepository petRepository,
                             VaccinationRepository vaccinationRepository, InvoiceRepository invoiceRepository,
                             InvoiceItemRepository invoiceItemRepository, VetRepository vetRepository) {
        this.visitRepository = visitRepository;
        this.petRepository = petRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.vetRepository = vetRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDateTime revenueWindowStart = currentMonth.minusMonths(REVENUE_MONTHS_WINDOW - 1).atDay(1).atStartOfDay();

        long todayAppointments = visitRepository.countByStatusNotAndScheduledAtBetween(
                VisitStatus.CANCELLED, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long activePatients = petRepository.countByArchivedFalse();
        long pendingVaccinations = vaccinationRepository.countByNextDueDateLessThanEqual(today);
        long unpaidInvoices = invoiceRepository.countByStatusNot(InvoiceStatus.PAID);
        List<MonthlyRevenueEntry> monthlyRevenue = buildMonthlyRevenue(currentMonth, revenueWindowStart);
        List<RevenueCategoryEntry> revenueByCategory = buildRevenueByCategory(revenueWindowStart);
        List<AppointmentTrendEntry> appointmentTrend30Days = buildAppointmentTrend30Days(today);
        List<Visit> visitsYtd = visitRepository.findByStatusNotAndScheduledAtBetween(
                VisitStatus.CANCELLED, today.withDayOfYear(1).atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<CumulativeAppointmentEntry> cumulativeAppointmentsYtd = buildCumulativeAppointmentsYtd(visitsYtd, today);
        List<VetAppointmentCountEntry> appointmentsByVet = buildAppointmentsByVet(visitsYtd);
        List<TodayScheduleEntry> todaySchedule = buildTodaySchedule(today);
        List<VaccinationAlertEntry> upcomingVaccinationAlerts = buildUpcomingVaccinationAlerts(today);
        List<FollowUpAlertEntry> overdueFollowUpAlerts = buildOverdueFollowUpAlerts(today);

        return new DashboardSummaryResponse(todayAppointments, activePatients, pendingVaccinations, unpaidInvoices,
                monthlyRevenue, revenueByCategory, appointmentTrend30Days, cumulativeAppointmentsYtd, appointmentsByVet,
                todaySchedule, upcomingVaccinationAlerts, overdueFollowUpAlerts);
    }

    private List<MonthlyRevenueEntry> buildMonthlyRevenue(YearMonth currentMonth, LocalDateTime windowStart) {
        Map<String, BigDecimal> revenueByMonth = invoiceRepository.findByIssuedAtGreaterThanEqual(windowStart).stream()
                .collect(Collectors.groupingBy(
                        invoice -> YearMonth.from(invoice.getIssuedAt()).format(MONTH_FORMATTER),
                        Collectors.reducing(BigDecimal.ZERO, Invoice::getSubtotal, BigDecimal::add)));

        List<MonthlyRevenueEntry> result = new ArrayList<>();
        for (int i = REVENUE_MONTHS_WINDOW - 1; i >= 0; i--) {
            String month = currentMonth.minusMonths(i).format(MONTH_FORMATTER);
            BigDecimal revenue = revenueByMonth.getOrDefault(month, BigDecimal.ZERO).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            result.add(new MonthlyRevenueEntry(month, revenue));
        }
        return result;
    }

    private List<RevenueCategoryEntry> buildRevenueByCategory(LocalDateTime windowStart) {
        Map<InvoiceItemCategory, BigDecimal> amountByCategory =
                invoiceItemRepository.findByInvoice_IssuedAtGreaterThanEqual(windowStart).stream()
                        .collect(Collectors.groupingBy(InvoiceItem::getCategory,
                                Collectors.reducing(BigDecimal.ZERO, InvoiceItem::getLineTotal, BigDecimal::add)));

        return Arrays.stream(InvoiceItemCategory.values())
                .map(category -> new RevenueCategoryEntry(category,
                        amountByCategory.getOrDefault(category, BigDecimal.ZERO).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)))
                .toList();
    }

    private List<AppointmentTrendEntry> buildAppointmentTrend30Days(LocalDate today) {
        LocalDate windowStart = today.minusDays(TREND_DAYS_WINDOW - 1);
        List<Visit> visits = visitRepository.findByStatusNotAndScheduledAtBetween(
                VisitStatus.CANCELLED, windowStart.atStartOfDay(), today.plusDays(1).atStartOfDay());

        Map<LocalDate, Long> countsByDay = visits.stream()
                .collect(Collectors.groupingBy(visit -> visit.getScheduledAt().toLocalDate(), Collectors.counting()));

        List<AppointmentTrendEntry> result = new ArrayList<>();
        for (int i = TREND_DAYS_WINDOW - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            result.add(new AppointmentTrendEntry(date, countsByDay.getOrDefault(date, 0L)));
        }
        return result;
    }

    private List<CumulativeAppointmentEntry> buildCumulativeAppointmentsYtd(List<Visit> visitsYtd, LocalDate today) {
        LocalDate yearStart = today.withDayOfYear(1);
        Map<YearMonth, Long> countsByMonth = visitsYtd.stream()
                .collect(Collectors.groupingBy(visit -> YearMonth.from(visit.getScheduledAt()), Collectors.counting()));

        List<CumulativeAppointmentEntry> result = new ArrayList<>();
        YearMonth currentMonth = YearMonth.from(today);
        long running = 0;
        for (YearMonth month = YearMonth.from(yearStart); !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            running += countsByMonth.getOrDefault(month, 0L);
            LocalDate snapshotDate = month.equals(currentMonth) ? today : month.atEndOfMonth();
            result.add(new CumulativeAppointmentEntry(snapshotDate, running));
        }
        return result;
    }

    private List<VetAppointmentCountEntry> buildAppointmentsByVet(List<Visit> visitsYtd) {
        Map<Long, Long> countsByVetId = visitsYtd.stream()
                .collect(Collectors.groupingBy(visit -> visit.getVet().getId(), Collectors.counting()));

        return vetRepository.findByActiveTrue().stream()
                .map(vet -> new VetAppointmentCountEntry(vet.getId(), vet.getName(),
                        countsByVetId.getOrDefault(vet.getId(), 0L)))
                .toList();
    }

    private List<TodayScheduleEntry> buildTodaySchedule(LocalDate today) {
        return visitRepository.findByStatusNotAndScheduledAtBetweenOrderByScheduledAtAsc(
                        VisitStatus.CANCELLED, today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .map(visit -> new TodayScheduleEntry(visit.getId(), visit.getPet().getName(), visit.getVet().getName(),
                        visit.getScheduledAt(), visit.getStatus()))
                .toList();
    }

    private List<VaccinationAlertEntry> buildUpcomingVaccinationAlerts(LocalDate today) {
        LocalDate windowEnd = today.plusDays(VACCINATION_ALERT_WINDOW_DAYS);
        return vaccinationRepository.findByNextDueDateLessThanEqualOrderByNextDueDateAsc(windowEnd).stream()
                .map(vaccination -> new VaccinationAlertEntry(vaccination.getPet().getId(), vaccination.getPet().getName(),
                        vaccination.getVaccineType(), vaccination.getNextDueDate()))
                .toList();
    }

    private List<FollowUpAlertEntry> buildOverdueFollowUpAlerts(LocalDate today) {
        return visitRepository.findByStatusAndFollowUpDateBeforeOrderByFollowUpDateAsc(VisitStatus.COMPLETED, today).stream()
                .map(visit -> new FollowUpAlertEntry(visit.getId(), visit.getPet().getName(), visit.getFollowUpDate()))
                .toList();
    }
}
