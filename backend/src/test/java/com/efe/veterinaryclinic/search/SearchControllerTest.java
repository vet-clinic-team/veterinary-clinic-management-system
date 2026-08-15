package com.efe.veterinaryclinic.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest {

    @Value("${app.seed.admin.email}")
    private String SEED_ADMIN_EMAIL;
    @Value("${app.seed.admin.password}")
    private String SEED_ADMIN_PASSWORD;
    @Value("${app.seed.receptionist.email}")
    private String SEED_RECEPTIONIST_EMAIL;
    @Value("${app.seed.receptionist.password}")
    private String SEED_RECEPTIONIST_PASSWORD;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchFindsOwnerPetAndVisitMatchingTheQuery() throws Exception {
        String token = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(token, "search-owner@example.com", "Search-Luna", "Ownerson");
        long petId = createPet(token, ownerId, "Search-Luna");
        long vetId = createVet();
        createVisit(token, petId, vetId, "2026-08-01T09:00:00");

        mockMvc.perform(get("/api/search").param("q", "search-luna").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners[0].firstName").value("Search-Luna"))
                .andExpect(jsonPath("$.pets[0].name").value("Search-Luna"))
                .andExpect(jsonPath("$.visits[0].petName").value("Search-Luna"));
    }

    @Test
    void searchCapsOwnerResultsAtFivePerCategory() throws Exception {
        String token = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String uniqueLastName = "SearchCap" + System.nanoTime();
        for (int i = 0; i < 6; i++) {
            createOwner(token, "search-cap-" + i + "-" + System.nanoTime() + "@example.com", "Owner" + i, uniqueLastName);
        }

        mockMvc.perform(get("/api/search").param("q", uniqueLastName).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners.length()").value(5));
    }

    @Test
    void blankQueryReturnsEmptyResults() throws Exception {
        String token = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/search").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners").isArray())
                .andExpect(jsonPath("$.owners").isEmpty())
                .andExpect(jsonPath("$.pets").isEmpty())
                .andExpect(jsonPath("$.visits").isEmpty());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "luna"))
                .andExpect(status().isUnauthorized());
    }

    private long createOwner(String token, String email, String firstName, String lastName) throws Exception {
        String ownerBody = objectMapper.writeValueAsString(
                new OwnerPayload(firstName, lastName, "+90 555 987 6543", email, "Ankara, Turkey"));

        String response = mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createPet(String token, long ownerId, String name) throws Exception {
        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, name, "CAT", null, null, "2023-01-10", "FEMALE", 4.2, null, null));

        String response = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createVet() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String vetBody = objectMapper.writeValueAsString(
                new VetPayload("Dr. Search Test", "General", "VET-LIC-SEARCH-001", "Mon-Fri 09:00-17:00"));

        String response = mockMvc.perform(post("/api/vets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vetBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createVisit(String token, long petId, long vetId, String scheduledAt) throws Exception {
        String visitBody = objectMapper.writeValueAsString(new VisitPayload(petId, vetId, scheduledAt, "Checkup"));

        String response = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitBody))
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

    private record OwnerPayload(String firstName, String lastName, String phone, String email, String address) {
    }

    private record PetPayload(Long ownerId, String name, String species, String breed, String speciesNote,
                               String birthDate, String sex, Double weightKg, String allergies,
                               String chronicConditions) {
    }

    private record VetPayload(String name, String specialty, String licenseNo, String workHours) {
    }

    private record VisitPayload(Long petId, Long vetId, String scheduledAt, String chiefComplaint) {
    }

    private record LoginPayload(String email, String password) {
    }
}
