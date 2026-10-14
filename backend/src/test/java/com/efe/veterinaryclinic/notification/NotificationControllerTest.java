
package com.efe.veterinaryclinic.notification;

import com.efe.veterinaryclinic.followup.FollowUp;
import com.efe.veterinaryclinic.followup.FollowUpRepository;
import com.efe.veterinaryclinic.followup.FollowUpRule;
import com.efe.veterinaryclinic.followup.FollowUpRuleRepository;
import com.efe.veterinaryclinic.followup.IntervalUnit;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
    private FollowUpRuleRepository followUpRuleRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private PetRepository petRepository;

    @Test
    void includesAppointmentStartingSoonVaccinationDueTodayAndNewlyCreatedRecords()
            throws Exception {

        String receptionistToken = loginAndGetToken(
                SEED_RECEPTIONIST_EMAIL,
                SEED_RECEPTIONIST_PASSWORD
        );

        String vetToken = loginAndGetToken(
                SEED_VET1_EMAIL,
                SEED_VET1_PASSWORD
        );

        long ownerId = createOwner(
                receptionistToken,
                "notification-owner-" + UUID.randomUUID() + "@example.com"
        );

        long petId = createPet(
                receptionistToken,
                ownerId,
                "Notify-Pati"
        );

        long vetId = createVet();

        createVisit(
                receptionistToken,
                petId,
                vetId,
                LocalDateTime.now().plusMinutes(30)
        );

        // Create a test rule for the automated follow-up system.
        // For DOG + ONE_DAY_TEST, the next vaccination is due after one day.
        FollowUpRule rule = new FollowUpRule(
                "DOG",
                "ONE_DAY_TEST",
                "Notification Test Rule"
        );

        rule.addStep(1, 1, IntervalUnit.DAY);

        followUpRuleRepository.saveAndFlush(rule);

        // A vaccination administered yesterday should be due today.
        LocalDateTime administeredAt = LocalDate.now()
                .minusDays(1)
                .atTime(10, 0);

        createVaccination(
                vetToken,
                petId,
                administeredAt
        );

        // Verify existing notifications and the new follow-up notification.
        mockMvc.perform(
                        get("/api/notifications")
                                .header(
                                        "Authorization",
                                        "Bearer " + receptionistToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.upcomingAppointments[?(@.petName=='Notify-Pati')]"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.vaccinationsDueToday[?(@.petName=='Notify-Pati')]"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.newRecords[?(@.label=='Notify-Pati')]"
                        ).exists()
                )
                .andExpect(
                        jsonPath("$.pendingFollowUps").isArray()
                )
                .andExpect(
                        jsonPath(
                                "$.pendingFollowUps[?(@.petName=='Notify-Pati' && @.procedureType=='ONE_DAY_TEST')]"
                        ).exists()
                );
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendingFollowUpNotificationsRespectSevenDayWindowAndStatus()
            throws Exception {

        String token = loginAndGetToken(
                SEED_RECEPTIONIST_EMAIL,
                SEED_RECEPTIONIST_PASSWORD
        );

        // Create independent test data for follow-up notifications.
        long ownerId = createOwner(
                token,
                "followup-notif-" + UUID.randomUUID() + "@example.com"
        );

        long petId = createPet(
                token,
                ownerId,
                "FollowUp-Notification-Test"
        );

        var pet = petRepository.findById(petId).orElseThrow();

        // An overdue pending follow-up must be visible.
        FollowUp overdue = followUpRepository.saveAndFlush(
                new FollowUp(
                        pet,
                        "OVERDUE_TEST",
                        LocalDate.now().minusDays(2),
                        "VACCINATION",
                        900001L + petId
                )
        );

        // A follow-up due exactly seven days from today must be visible.
        FollowUp boundary = followUpRepository.saveAndFlush(
                new FollowUp(
                        pet,
                        "SEVEN_DAY_TEST",
                        LocalDate.now().plusDays(7),
                        "VACCINATION",
                        900002L + petId
                )
        );

        // A follow-up due after the seven-day window must be hidden.
        FollowUp future = followUpRepository.saveAndFlush(
                new FollowUp(
                        pet,
                        "FUTURE_TEST",
                        LocalDate.now().plusDays(8),
                        "VACCINATION",
                        900003L + petId
                )
        );

        // Manually completed follow-ups must not appear in notifications.
        FollowUp completed = new FollowUp(
                pet,
                "COMPLETED_TEST",
                LocalDate.now().plusDays(2),
                "VACCINATION",
                900004L + petId
        );

        completed.markManuallyCompleted();
        completed = followUpRepository.saveAndFlush(completed);

        // Cancelled follow-ups must not appear in notifications.
        FollowUp cancelled = new FollowUp(
                pet,
                "CANCELLED_TEST",
                LocalDate.now().plusDays(2),
                "VACCINATION",
                900005L + petId
        );

        cancelled.cancel();
        cancelled = followUpRepository.saveAndFlush(cancelled);

        // Retrieve notifications using an authorized receptionist account.
        String response = mockMvc.perform(
                        get("/api/notifications")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode notifications = objectMapper.readTree(response)
                .get("pendingFollowUps");

        // Pending follow-ups within the allowed window must be included.
        assertTrue(containsFollowUp(notifications, overdue.getId()));
        assertTrue(containsFollowUp(notifications, boundary.getId()));

        // Future, completed, and cancelled follow-ups must be excluded.
        assertFalse(containsFollowUp(notifications, future.getId()));
        assertFalse(containsFollowUp(notifications, completed.getId()));
        assertFalse(containsFollowUp(notifications, cancelled.getId()));
    }

    private boolean containsFollowUp(
            JsonNode notifications,
            Long followUpId
    ) {
        for (JsonNode notification : notifications) {
            if (notification.get("followUpId").asLong() == followUpId) {
                return true;
            }
        }

        return false;
    }

    private long createOwner(String token, String email)
            throws Exception {

        String ownerBody = objectMapper.writeValueAsString(
                new OwnerPayload(
                        "Notify",
                        "Owner",
                        "+90 555 222 3344",
                        email,
                        "Izmir, Turkey"
                )
        );

        String response = mockMvc.perform(
                        post("/api/owners")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ownerBody)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    private long createPet(
            String token,
            long ownerId,
            String name
    ) throws Exception {

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(
                        ownerId,
                        name,
                        "DOG",
                        "Labrador",
                        null,
                        "2022-05-20",
                        "MALE",
                        20.0,
                        null,
                        null
                )
        );

        String response = mockMvc.perform(
                        post("/api/pets")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(petBody)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    private long createVet() throws Exception {

        String adminToken = loginAndGetToken(
                SEED_ADMIN_EMAIL,
                SEED_ADMIN_PASSWORD
        );

        String vetBody = objectMapper.writeValueAsString(
                new VetPayload(
                        "Dr. Notification Test",
                        "General",
                        "VET-LIC-NOTIF-" + UUID.randomUUID(),
                        "Mon-Fri 09:00-17:00"
                )
        );

        String response = mockMvc.perform(
                        post("/api/vets")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(vetBody)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    private void createVisit(
            String token,
            long petId,
            long vetId,
            LocalDateTime scheduledAt
    ) throws Exception {

        String visitBody = objectMapper.writeValueAsString(
                new VisitPayload(
                        petId,
                        vetId,
                        scheduledAt.format(ISO),
                        "Checkup"
                )
        );

        mockMvc.perform(
                        post("/api/visits")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(visitBody)
                )
                .andExpect(status().isCreated());
    }

    private void createVaccination(
            String token,
            long petId,
            LocalDateTime administeredAt
    ) throws Exception {

        String vaccinationBody = objectMapper.writeValueAsString(
                new VaccinationPayload(
                        petId,
                        "ONE_DAY_TEST",
                        administeredAt.format(ISO),
                        "LOT-NOTIF",
                        "Dr. Vet"
                )
        );

        mockMvc.perform(
                        post("/api/vaccinations")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(vaccinationBody)
                )
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(
            String email,
            String password
    ) throws Exception {

        String loginBody = objectMapper.writeValueAsString(
                new LoginPayload(email, password)
        );

        String response = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("token")
                .asText();
    }

    private record OwnerPayload(
            String firstName,
            String lastName,
            String phone,
            String email,
            String address
    ) {
    }

    private record PetPayload(
            Long ownerId,
            String name,
            String species,
            String breed,
            String speciesNote,
            String birthDate,
            String sex,
            Double weightKg,
            String allergies,
            String chronicConditions
    ) {
    }

    private record VetPayload(
            String name,
            String specialty,
            String licenseNo,
            String workHours
    ) {
    }

    private record VisitPayload(
            Long petId,
            Long vetId,
            String scheduledAt,
            String chiefComplaint
    ) {
    }

    private record VaccinationPayload(
            Long petId,
            String vaccineType,
            String administeredAt,
            String lotNumber,
            String administeredBy
    ) {
    }

    private record LoginPayload(
            String email,
            String password
    ) {
    }
}


