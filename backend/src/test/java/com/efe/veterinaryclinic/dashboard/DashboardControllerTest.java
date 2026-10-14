
package com.efe.veterinaryclinic.dashboard;

import com.efe.veterinaryclinic.followup.FollowUp;
import com.efe.veterinaryclinic.followup.FollowUpRepository;
import com.efe.veterinaryclinic.owner.Owner;
import com.efe.veterinaryclinic.owner.OwnerRepository;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    @Value("${app.seed.admin.email}")
    private String SEED_ADMIN_EMAIL;

    @Value("${app.seed.admin.password}")
    private String SEED_ADMIN_PASSWORD;

    @Value("${app.seed.vet1.email}")
    private String SEED_VET1_EMAIL;

    @Value("${app.seed.vet1.password}")
    private String SEED_VET1_PASSWORD;

    @Value("${app.seed.receptionist.email}")
    private String SEED_RECEPTIONIST_EMAIL;

    @Value("${app.seed.receptionist.password}")
    private String SEED_RECEPTIONIST_PASSWORD;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    // Existing tests

    @Test
    void adminGetsFullDashboardSummary() throws Exception {
        String adminToken = loginAndGetToken(
                SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayAppointments").exists())
                .andExpect(jsonPath("$.activePatients").exists())
                .andExpect(jsonPath("$.pendingVaccinations").exists())
                .andExpect(jsonPath("$.unpaidInvoices").exists())
                .andExpect(jsonPath("$.monthlyRevenue").isArray())
                .andExpect(jsonPath("$.revenueByCategory").isArray())
                .andExpect(jsonPath("$.appointmentTrend30Days").isArray())
                .andExpect(jsonPath("$.cumulativeAppointmentsYtd").isArray())
                .andExpect(jsonPath("$.appointmentsByVet").isArray())
                .andExpect(jsonPath("$.todaySchedule").isArray())
                .andExpect(jsonPath("$.upcomingVaccinationAlerts").isArray())
                .andExpect(jsonPath("$.overdueFollowUpAlerts").isArray())
                .andExpect(jsonPath("$.pendingFollowUpAlerts").isArray());
    }

    @Test
    void vetCanReadDashboardSummary() throws Exception {
        String vetToken = loginAndGetToken(
                SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isOk());
    }

    @Test
    void receptionistCanReadDashboardSummary() throws Exception {
        String receptionistToken = loginAndGetToken(
                SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    // New integration tests

    @Test
    void pendingFollowUpAppearsOnDashboard() throws Exception {
        String token = loginAndGetToken(
                SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        FollowUp followUp = createTestFollowUp();

        JsonNode alerts = getPendingAlerts(token);

        assertTrue(containsFollowUp(alerts, followUp.getId()));
    }

    @Test
    void completedFollowUpDoesNotAppearOnDashboard() throws Exception {
        String token = loginAndGetToken(
                SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        FollowUp followUp = createTestFollowUp();

        followUp.markManuallyCompleted();
        followUpRepository.save(followUp);

        JsonNode alerts = getPendingAlerts(token);

        assertFalse(containsFollowUp(alerts, followUp.getId()));
    }

    @Test
    void cancelledFollowUpDoesNotAppearOnDashboard() throws Exception {
        String token = loginAndGetToken(
                SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        FollowUp followUp = createTestFollowUp();

        followUp.cancel();
        followUpRepository.save(followUp);

        JsonNode alerts = getPendingAlerts(token);

        assertFalse(containsFollowUp(alerts, followUp.getId()));
    }

    // Helpers

    private FollowUp createTestFollowUp() {

        String uniqueId = UUID.randomUUID().toString();

        Owner owner = ownerRepository.save(
                new Owner(
                        "Dashboard",
                        "Test",
                        "1234567890",
                        "dashboard-" + uniqueId + "@example.com",
                        "Test Address"
                )
        );

        Pet pet = petRepository.save(
                new Pet(
                        owner,
                        "Milo",
                        "CAT",
                        "Mixed",
                        null,
                        LocalDate.of(2023, 5, 10),
                        "MALE",
                        4.5,
                        null,
                        null
                )
        );

        FollowUp followUp = new FollowUp(
                pet,
                "TEST_VACCINE",
                LocalDate.now().plusDays(7),
                "VACCINATION",
                900000L + pet.getId()
        );

        return followUpRepository.save(followUp);
    }

    private JsonNode getPendingAlerts(String token) throws Exception {

        String response = mockMvc.perform(
                        get("/api/dashboard/summary")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("pendingFollowUpAlerts");
    }

    private boolean containsFollowUp(JsonNode alerts, Long followUpId) {

        for (JsonNode alert : alerts) {
            if (alert.get("followUpId").asLong() == followUpId) {
                return true;
            }
        }

        return false;
    }

    private String loginAndGetToken(
            String email,
            String password) throws Exception {

        String loginBody = objectMapper.writeValueAsString(
                new LoginPayload(email, password));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("token")
                .asText();
    }

    private record LoginPayload(
            String email,
            String password) {
    }
}
