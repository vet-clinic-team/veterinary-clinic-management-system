
package com.efe.veterinaryclinic.notification;

import com.efe.veterinaryclinic.followup.FollowUpRule;
import com.efe.veterinaryclinic.followup.FollowUpRuleRepository;
import com.efe.veterinaryclinic.followup.IntervalUnit;
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
                "notification-owner@example.com"
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

        // Yeni otomasyon sistemine uygun test kuralı.
        // DOG + ONE_DAY_TEST için sonraki aşı 1 gün sonra.
        FollowUpRule rule = new FollowUpRule(
                "DOG",
                "ONE_DAY_TEST",
                "Notification Test Rule"
        );

        rule.addStep(1, 1, IntervalUnit.DAY);

        followUpRuleRepository.saveAndFlush(rule);

        // Aşı dün yapıldıysa sonraki aşı tarihi bugün olmalı.
        LocalDateTime administeredAt = LocalDate.now()
                .minusDays(1)
                .atTime(10, 0);

        createVaccination(
                vetToken,
                petId,
                administeredAt
        );

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
                );
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
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
                        "VET-LIC-NOTIF-001",
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

