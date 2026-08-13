package com.efe.veterinaryclinic.dashboard;

import com.efe.veterinaryclinic.dashboard.dto.DashboardSummaryResponse;
import com.efe.veterinaryclinic.dashboard.dto.VaccinationAlertEntry;
import com.efe.veterinaryclinic.visit.VisitStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardServiceTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Value("${app.seed.admin.email}")
    private String SEED_ADMIN_EMAIL;
    @Value("${app.seed.admin.password}")
    private String SEED_ADMIN_PASSWORD;
    @Value("${app.seed.receptionist.email}")
    private String SEED_RECEPTIONIST_EMAIL;
    @Value("${app.seed.receptionist.password}")
    private String SEED_RECEPTIONIST_PASSWORD;
    @Value("${app.seed.vet1.email}")
    private String SEED_VET1_EMAIL;
    @Value("${app.seed.vet1.password}")
    private String SEED_VET1_PASSWORD;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DashboardService dashboardService;

    @Test
    void todayAppointmentsCountsOnlyNonCancelledVisitsScheduledToday() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long baseline = dashboardService.getSummary().todayAppointments();

        long petId = createPet(receptionistToken, "dashboard-today@example.com", "Boncuk");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-001");
        long todayVisitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().withHour(9).withMinute(0));

        assertThat(dashboardService.getSummary().todayAppointments()).isEqualTo(baseline + 1);

        mockMvc.perform(patch("/api/visits/" + todayVisitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload("CANCELLED"))))
                .andExpect(status().isOk());

        assertThat(dashboardService.getSummary().todayAppointments()).isEqualTo(baseline);
    }

    @Test
    void todayAppointmentsExcludesVisitsScheduledOnOtherDays() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long baseline = dashboardService.getSummary().todayAppointments();

        long petId = createPet(receptionistToken, "dashboard-tomorrow@example.com", "Minnos");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-002");
        createVisit(receptionistToken, petId, vetId, LocalDateTime.now().plusDays(2).withHour(9).withMinute(0));

        assertThat(dashboardService.getSummary().todayAppointments()).isEqualTo(baseline);
    }

    @Test
    void activePatientsCountsOnlyNonArchivedPets() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long baseline = dashboardService.getSummary().activePatients();

        long petId = createPet(receptionistToken, "dashboard-active@example.com", "Tekir");

        assertThat(dashboardService.getSummary().activePatients()).isEqualTo(baseline + 1);

        mockMvc.perform(patch("/api/pets/" + petId + "/archive")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());

        assertThat(dashboardService.getSummary().activePatients()).isEqualTo(baseline);
    }

    @Test
    void pendingVaccinationsCountsVaccinationsWithNextDueDateTodayOrEarlier() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long baseline = dashboardService.getSummary().pendingVaccinations();

        long petId = createPet(receptionistToken, "dashboard-vaccination@example.com", "Duman");

        // ONE_YEAR type administered over a year ago -> nextDueDate is already in the past
        String overdueBody = objectMapper.writeValueAsString(new VaccinationPayload(
                petId, "ONE_YEAR", LocalDateTime.now().minusYears(1).minusDays(1).format(ISO), "LOT-1", "Dr. Vet"));
        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overdueBody))
                .andExpect(status().isCreated());

        assertThat(dashboardService.getSummary().pendingVaccinations()).isEqualTo(baseline + 1);

        // ONE_YEAR type administered today -> nextDueDate is a year away, not pending
        String futureBody = objectMapper.writeValueAsString(new VaccinationPayload(
                petId, "ONE_YEAR", LocalDateTime.now().format(ISO), "LOT-2", "Dr. Vet"));
        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(futureBody))
                .andExpect(status().isCreated());

        assertThat(dashboardService.getSummary().pendingVaccinations()).isEqualTo(baseline + 1);
    }

    @Test
    void unpaidInvoicesExcludesPaidInvoices() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long baseline = dashboardService.getSummary().unpaidInvoices();

        long petId = createPet(receptionistToken, "dashboard-invoice@example.com", "Karabas");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-003");
        long visitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().withHour(9).withMinute(0));

        String invoiceBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));
        String invoiceResponse = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long invoiceId = objectMapper.readTree(invoiceResponse).get("id").asLong();

        assertThat(dashboardService.getSummary().unpaidInvoices()).isEqualTo(baseline + 1);

        mockMvc.perform(patch("/api/invoices/" + invoiceId + "/mark-paid")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());

        assertThat(dashboardService.getSummary().unpaidInvoices()).isEqualTo(baseline);
    }

    @Test
    void monthlyRevenueIncludesCurrentMonthSubtotalFromIssuedInvoices() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String currentMonthKey = YearMonth.now().format(MONTH_FORMAT);
        BigDecimal baseline = revenueForMonth(dashboardService.getSummary(), currentMonthKey);

        long petId = createPet(receptionistToken, "dashboard-revenue@example.com", "Pati");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-004");
        long visitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().withHour(9).withMinute(0));

        String invoiceBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody))
                .andExpect(status().isCreated());

        DashboardSummaryResponse summary = dashboardService.getSummary();
        assertThat(summary.monthlyRevenue()).hasSize(12);
        assertThat(summary.monthlyRevenue().get(11).month()).isEqualTo(currentMonthKey);
        // monthlyRevenue is VAT-exclusive (subtotal), matching revenueByCategory's basis
        assertThat(revenueForMonth(summary, currentMonthKey)).isEqualByComparingTo(baseline.add(new BigDecimal("500.00")));
    }

    @Test
    void revenueByCategoryIncludesAllCategoriesAndAccumulatesLineTotals() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        BigDecimal baseline = categoryAmount(dashboardService.getSummary(), "VACCINATION");

        long petId = createPet(receptionistToken, "dashboard-category@example.com", "Zorro");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-005");
        long visitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().withHour(9).withMinute(0));

        String invoiceBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Rabies vaccine", "VACCINATION", 1, new BigDecimal("300.00")))));
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody))
                .andExpect(status().isCreated());

        DashboardSummaryResponse summary = dashboardService.getSummary();
        assertThat(summary.revenueByCategory()).hasSize(5);
        assertThat(categoryAmount(summary, "VACCINATION")).isEqualByComparingTo(baseline.add(new BigDecimal("300.00")));
    }

    private BigDecimal revenueForMonth(DashboardSummaryResponse summary, String month) {
        return summary.monthlyRevenue().stream()
                .filter(entry -> entry.month().equals(month))
                .findFirst()
                .orElseThrow()
                .revenue();
    }

    private BigDecimal categoryAmount(DashboardSummaryResponse summary, String category) {
        return summary.revenueByCategory().stream()
                .filter(entry -> entry.category().name().equals(category))
                .findFirst()
                .orElseThrow()
                .amount();
    }

    @Test
    void appointmentTrend30DaysCountsOnlyNonCancelledVisitsScheduledToday() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        LocalDate today = LocalDate.now();
        long baseline = trendCountForDay(dashboardService.getSummary(), today);

        long petId = createPet(receptionistToken, "dashboard-trend@example.com", "Bella");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-006");
        long visitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().withHour(9).withMinute(0));

        DashboardSummaryResponse summary = dashboardService.getSummary();
        assertThat(summary.appointmentTrend30Days()).hasSize(30);
        assertThat(summary.appointmentTrend30Days().get(29).date()).isEqualTo(today);
        assertThat(trendCountForDay(summary, today)).isEqualTo(baseline + 1);

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload("CANCELLED"))))
                .andExpect(status().isOk());

        assertThat(trendCountForDay(dashboardService.getSummary(), today)).isEqualTo(baseline);
    }

    @Test
    void cumulativeAppointmentsYtdIncreasesForCurrentMonthWhenVisitScheduledToday() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        DashboardSummaryResponse before = dashboardService.getSummary();
        long baseline = before.cumulativeAppointmentsYtd().get(before.cumulativeAppointmentsYtd().size() - 1).cumulativeCount();

        long petId = createPet(receptionistToken, "dashboard-ytd@example.com", "Rocky");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-007");
        createVisit(receptionistToken, petId, vetId, LocalDateTime.now().withHour(9).withMinute(0));

        DashboardSummaryResponse after = dashboardService.getSummary();
        int lastIndex = after.cumulativeAppointmentsYtd().size() - 1;
        assertThat(after.cumulativeAppointmentsYtd().get(lastIndex).date()).isEqualTo(LocalDate.now());
        assertThat(after.cumulativeAppointmentsYtd().get(lastIndex).cumulativeCount()).isEqualTo(baseline + 1);
        assertThat(after.cumulativeAppointmentsYtd()).hasSize(YearMonth.now().getMonthValue());
    }

    @Test
    void appointmentsByVetCountsOnlyNonCancelledVisitsForThatVetYtd() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "dashboard-by-vet@example.com", "Fistik");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-008");
        long baseline = vetAppointmentCount(dashboardService.getSummary(), vetId);

        long visitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().withHour(9).withMinute(0));

        DashboardSummaryResponse summary = dashboardService.getSummary();
        assertThat(summary.appointmentsByVet()).anyMatch(entry -> entry.vetId().equals(vetId));
        assertThat(vetAppointmentCount(summary, vetId)).isEqualTo(baseline + 1);

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload("CANCELLED"))))
                .andExpect(status().isOk());

        assertThat(vetAppointmentCount(dashboardService.getSummary(), vetId)).isEqualTo(baseline);
    }

    @Test
    void todayScheduleIncludesTodaysVisitsAndExcludesCancelledOnes() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "dashboard-schedule@example.com", "Sultan");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-009");
        LocalDateTime scheduledAt = LocalDateTime.now().withHour(10).withMinute(0);
        long visitId = createVisit(receptionistToken, petId, vetId, scheduledAt);

        DashboardSummaryResponse summary = dashboardService.getSummary();
        assertThat(summary.todaySchedule()).anyMatch(entry -> entry.visitId().equals(visitId)
                && entry.petName().equals("Sultan") && entry.status() == VisitStatus.SCHEDULED);

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload("CANCELLED"))))
                .andExpect(status().isOk());

        assertThat(dashboardService.getSummary().todaySchedule()).noneMatch(entry -> entry.visitId().equals(visitId));
    }

    @Test
    void upcomingVaccinationAlertsIncludesOnlyVaccinationsDueWithin30Days() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "dashboard-vaccination-alert@example.com", "Leyla");

        // ONE_YEAR administered 340 days ago -> nextDueDate ~25 days away, inside the 30-day window
        String withinWindowBody = objectMapper.writeValueAsString(new VaccinationPayload(
                petId, "ONE_YEAR", LocalDateTime.now().minusDays(340).format(ISO), "LOT-A", "Dr. Vet"));
        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withinWindowBody))
                .andExpect(status().isCreated());

        // ONE_YEAR administered today -> nextDueDate a year away, outside the 30-day window
        String outsideWindowBody = objectMapper.writeValueAsString(new VaccinationPayload(
                petId, "ONE_YEAR", LocalDateTime.now().format(ISO), "LOT-B", "Dr. Vet"));
        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outsideWindowBody))
                .andExpect(status().isCreated());

        List<VaccinationAlertEntry> alerts = dashboardService.getSummary().upcomingVaccinationAlerts();
        assertThat(alerts).anyMatch(entry -> entry.petId().equals(petId)
                && !entry.nextDueDate().isAfter(LocalDate.now().plusDays(30)));
        assertThat(alerts).noneMatch(entry -> entry.petId().equals(petId)
                && entry.nextDueDate().isAfter(LocalDate.now().plusDays(30)));
    }

    @Test
    void overdueFollowUpAlertsIncludesOnlyCompletedVisitsWithPastFollowUpDate() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "dashboard-followup@example.com", "Baris");
        long vetId = createVet(receptionistToken, "VET-LIC-DASH-010");
        long visitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().minusDays(10).withHour(9).withMinute(0));

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusPayload("COMPLETED"))))
                .andExpect(status().isOk());

        assertThat(dashboardService.getSummary().overdueFollowUpAlerts()).noneMatch(entry -> entry.visitId().equals(visitId));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MedicalNotesPayload("Diagnosis", "Notes", LocalDate.now().minusDays(1).toString()))))
                .andExpect(status().isOk());

        DashboardSummaryResponse summary = dashboardService.getSummary();
        assertThat(summary.overdueFollowUpAlerts()).anyMatch(entry -> entry.visitId().equals(visitId)
                && entry.petName().equals("Baris"));
    }

    private long vetAppointmentCount(DashboardSummaryResponse summary, long vetId) {
        return summary.appointmentsByVet().stream()
                .filter(entry -> entry.vetId().equals(vetId))
                .findFirst()
                .orElseThrow()
                .count();
    }

    private long trendCountForDay(DashboardSummaryResponse summary, LocalDate day) {
        return summary.appointmentTrend30Days().stream()
                .filter(entry -> entry.date().equals(day))
                .findFirst()
                .orElseThrow()
                .count();
    }

    private long createVisit(String token, long petId, long vetId, LocalDateTime scheduledAt) throws Exception {
        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, scheduledAt.format(ISO), "Checkup"));

        String response = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createPet(String token, String ownerEmail, String petName) throws Exception {
        long ownerId = createOwner(token, ownerEmail);

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, petName, "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        String response = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createOwner(String token, String email) throws Exception {
        String ownerBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", email, "Istanbul, Turkey"));

        String response = mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createVet(String token, String licenseNo) throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String vetBody = objectMapper.writeValueAsString(
                new VetPayload("Dr. Dashboard Test", "Surgery", licenseNo, "Mon-Fri 09:00-17:00"));

        String response = mockMvc.perform(post("/api/vets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vetBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, password));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private record VisitPayload(Long petId, Long vetId, String scheduledAt, String chiefComplaint) {
    }

    private record StatusPayload(String status) {
    }

    private record VaccinationPayload(Long petId, String vaccineType, String administeredAt,
                                       String lotNumber, String administeredBy) {
    }

    private record InvoicePayload(Long visitId, List<InvoiceItemPayload> items) {
    }

    private record InvoiceItemPayload(String description, String category, Integer quantity, BigDecimal unitPrice) {
    }

    private record MedicalNotesPayload(String diagnosis, String treatmentNotes, String followUpDate) {
    }

    private record PetPayload(Long ownerId, String name, String species, String breed, String speciesNote,
                               String birthDate, String sex, Double weightKg, String allergies,
                               String chronicConditions) {
    }

    private record OwnerPayload(String firstName, String lastName, String phone, String email, String address) {
    }

    private record VetPayload(String name, String specialty, String licenseNo, String workHours) {
    }

    private record LoginPayload(String email, String password) {
    }
}
