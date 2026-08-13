package com.efe.veterinaryclinic.visit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VisitReminderSchedulerTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${app.seed.receptionist.email}")
    private String SEED_RECEPTIONIST_EMAIL;
    @Value("${app.seed.receptionist.password}")
    private String SEED_RECEPTIONIST_PASSWORD;
    @Value("${app.seed.admin.email}")
    private String SEED_ADMIN_EMAIL;
    @Value("${app.seed.admin.password}")
    private String SEED_ADMIN_PASSWORD;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VisitReminderScheduler visitReminderScheduler;

    @MockitoBean
    private VisitReminderNotifier visitReminderNotifier;

    @Test
    void schedulerMarksTomorrowsScheduledVisitAsRemindedWhenEmailIsActuallySent() throws Exception {
        when(visitReminderNotifier.notifyUpcomingVisit(any())).thenReturn(true);

        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "reminder-tomorrow@example.com", "Tarcin");
        long vetId = createVet(receptionistToken, "VET-LIC-REMINDER-001"); // gitleaks:allow
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        long visitId = createVisit(receptionistToken, petId, vetId, scheduledAt);

        getVisit(receptionistToken, visitId).andExpect(jsonPath("$.reminderSentAt").doesNotExist());

        visitReminderScheduler.sendTomorrowReminders();

        getVisit(receptionistToken, visitId).andExpect(jsonPath("$.reminderSentAt").exists());
    }

    @Test
    void schedulerDoesNotMarkVisitAsRemindedWhenEmailWasNotActuallySent() throws Exception {
        when(visitReminderNotifier.notifyUpcomingVisit(any())).thenReturn(false);

        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "reminder-not-sent@example.com", "Findik");
        long vetId = createVet(receptionistToken, "VET-LIC-REMINDER-005"); // gitleaks:allow
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        long visitId = createVisit(receptionistToken, petId, vetId, scheduledAt);

        visitReminderScheduler.sendTomorrowReminders();

        getVisit(receptionistToken, visitId).andExpect(jsonPath("$.reminderSentAt").doesNotExist());
    }

    @Test
    void schedulerDoesNotMarkVisitScheduledOutsideTomorrowsWindow() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "reminder-nextweek@example.com", "Duman");
        long vetId = createVet(receptionistToken, "VET-LIC-REMINDER-002"); // gitleaks:allow
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
        long visitId = createVisit(receptionistToken, petId, vetId, scheduledAt);

        visitReminderScheduler.sendTomorrowReminders();

        getVisit(receptionistToken, visitId).andExpect(jsonPath("$.reminderSentAt").doesNotExist());
    }

    @Test
    void schedulerDoesNotMarkCancelledVisitScheduledTomorrow() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "reminder-cancelled@example.com", "Boncuk");
        long vetId = createVet(receptionistToken, "VET-LIC-REMINDER-003"); // gitleaks:allow
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);
        long visitId = createVisit(receptionistToken, petId, vetId, scheduledAt);

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        visitReminderScheduler.sendTomorrowReminders();

        getVisit(receptionistToken, visitId).andExpect(jsonPath("$.reminderSentAt").doesNotExist());
    }

    @Test
    void runningSchedulerTwiceIsIdempotent() throws Exception {
        when(visitReminderNotifier.notifyUpcomingVisit(any())).thenReturn(true);

        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "reminder-idempotent@example.com", "Zeytin");
        long vetId = createVet(receptionistToken, "VET-LIC-REMINDER-004"); // gitleaks:allow
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1).withHour(13).withMinute(0).withSecond(0).withNano(0);
        long visitId = createVisit(receptionistToken, petId, vetId, scheduledAt);

        visitReminderScheduler.sendTomorrowReminders();
        String firstReminderSentAt = objectMapper.readTree(
                        getVisit(receptionistToken, visitId).andReturn().getResponse().getContentAsString())
                .get("reminderSentAt").asText();

        visitReminderScheduler.sendTomorrowReminders();
        String secondReminderSentAt = objectMapper.readTree(
                        getVisit(receptionistToken, visitId).andReturn().getResponse().getContentAsString())
                .get("reminderSentAt").asText();

        assertThat(secondReminderSentAt).isEqualTo(firstReminderSentAt);
    }

    private org.springframework.test.web.servlet.ResultActions getVisit(String token, long visitId) throws Exception {
        return mockMvc.perform(get("/api/visits/" + visitId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
                new VetPayload("Dr. Reminder Test", "General", licenseNo, "Mon-Fri 09:00-17:00"));

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
