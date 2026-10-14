
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.owner.Owner;
import com.efe.veterinaryclinic.owner.OwnerRepository;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FollowUpControllerTest {

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

    @Value("${app.seed.admin.email}")
    private String adminEmail;

    @Value("${app.seed.admin.password}")
    private String adminPassword;

    @Value("${app.seed.receptionist.email}")
    private String receptionistEmail;

    @Value("${app.seed.receptionist.password}")
    private String receptionistPassword;

    @Value("${app.seed.vet1.email}")
    private String vetEmail;

    @Value("${app.seed.vet1.password}")
    private String vetPassword;

    // Existing tests

    @Test
    void adminCanListPendingFollowUps() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);

        mockMvc.perform(get("/api/follow-ups/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void receptionistCanListPendingFollowUps() throws Exception {
        String token = loginAndGetToken(
                receptionistEmail, receptionistPassword);

        mockMvc.perform(get("/api/follow-ups/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void vetCanListPendingFollowUps() throws Exception {
        String token = loginAndGetToken(vetEmail, vetPassword);

        mockMvc.perform(get("/api/follow-ups/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void receptionistCannotCompleteFollowUp() throws Exception {
        String token = loginAndGetToken(
                receptionistEmail, receptionistPassword);

        mockMvc.perform(patch("/api/follow-ups/999999/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCompletingUnknownFollowUpReturnsNotFound() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);

        mockMvc.perform(patch("/api/follow-ups/999999/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedUserCannotListFollowUps() throws Exception {
        mockMvc.perform(get("/api/follow-ups/pending"))
                .andExpect(status().isUnauthorized());
    }

    // New integration tests

    @Test
    void adminCanManuallyCompleteFollowUp() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);
        FollowUp followUp = createTestFollowUp();

        mockMvc.perform(patch("/api/follow-ups/" + followUp.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(followUp.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completionType").value("MANUAL"));

        FollowUp saved = followUpRepository.findById(followUp.getId())
                .orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(FollowUpStatus.COMPLETED);
        assertThat(saved.getCompletionType()).isEqualTo(FollowUpCompletionType.MANUAL);
    }

    @Test
    void vetCanManuallyCompleteFollowUp() throws Exception {
        String token = loginAndGetToken(vetEmail, vetPassword);
        FollowUp followUp = createTestFollowUp();

        mockMvc.perform(patch("/api/follow-ups/" + followUp.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completionType").value("MANUAL"));
    }

    @Test
    void completingAlreadyCompletedFollowUpReturnsConflict() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);
        FollowUp followUp = createTestFollowUp();

        mockMvc.perform(patch("/api/follow-ups/" + followUp.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/follow-ups/" + followUp.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void receptionistCanListFollowUpsByPet() throws Exception {
        String token = loginAndGetToken(
                receptionistEmail, receptionistPassword);

        FollowUp followUp = createTestFollowUp();
        Long petId = followUp.getPet().getId();

        mockMvc.perform(get("/api/follow-ups/pet/" + petId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(followUp.getId()))
                .andExpect(jsonPath("$[0].petId").value(petId));
    }

    @Test
    void manuallyCompletedFollowUpIsNotInPendingList() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);
        FollowUp followUp = createTestFollowUp();

        mockMvc.perform(patch("/api/follow-ups/" + followUp.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String response = mockMvc.perform(get("/api/follow-ups/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(response).toString())
                .doesNotContain("\"id\":" + followUp.getId() + ",");
    }

    // Test data helpers

    private FollowUp createTestFollowUp() {

        String uniqueId = UUID.randomUUID().toString();

        Owner owner = ownerRepository.save(
                new Owner(
                        "Test",
                        "Owner",
                        "1234567890",
                        "followup-" + uniqueId + "@example.com",
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
                LocalDate.of(2027, 1, 10),
                "VACCINATION",
                900000L + pet.getId()
        );

        return followUpRepository.save(followUp);
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


