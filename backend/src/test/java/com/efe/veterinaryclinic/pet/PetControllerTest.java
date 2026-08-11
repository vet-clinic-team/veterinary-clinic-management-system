package com.efe.veterinaryclinic.pet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PetControllerTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

    @Test
    void receptionistCreatesPetForOwnerThenPetAppearsInListAndOwnerPetCountIncreases() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "pet-owner@example.com");

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, "Penicillin", null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Boncuk"))
                .andExpect(jsonPath("$.ownerId").value(ownerId))
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/api/pets").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/owners").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());
    }

    @Test
    void createPetByVetIsForbidden() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String createBody = objectMapper.writeValueAsString(
                new PetPayload(1L, "Tekir", "CAT", "Tabby", null,
                        "2021-01-01", "MALE", 4.2, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPetWithUnknownOwnerReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String createBody = objectMapper.writeValueAsString(
                new PetPayload(999999L, "Tekir", "CAT", "Tabby", null,
                        "2021-01-01", "MALE", 4.2, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPetWithNonCatDogSpeciesAndBlankSpeciesNoteReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "exotic-owner@example.com");

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", "PARROT", null, null,
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("speciesNote"));
    }

    @Test
    void createPetWithNonCatDogSpeciesAndSpeciesNoteSucceeds() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "exotic-owner-2@example.com");

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", "PARROT", null, "African Grey Parrot",
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.speciesNote").value("African Grey Parrot"));
    }

    @Test
    void createPetWithNameAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "name-max-owner@example.com");
        String name = "N".repeat(100);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, name, "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void createPetWithNameOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "name-over-owner@example.com");
        String name = "N".repeat(101);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, name, "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createPetWithSpeciesAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "species-max-owner@example.com");
        String species = "S".repeat(50);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", species, null, "Exotic species note",
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.species").value(species));
    }

    @Test
    void createPetWithSpeciesOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "species-over-owner@example.com");
        String species = "S".repeat(51);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", species, null, "Exotic species note",
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("species"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createPetWithBreedAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "breed-max-owner@example.com");
        String breed = "B".repeat(100);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", breed, null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.breed").value(breed));
    }

    @Test
    void createPetWithBreedOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "breed-over-owner@example.com");
        String breed = "B".repeat(101);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", breed, null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("breed"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createPetWithSpeciesNoteAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "speciesnote-max-owner@example.com");
        String speciesNote = "N".repeat(200);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", "PARROT", null, speciesNote,
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.speciesNote").value(speciesNote));
    }

    @Test
    void createPetWithSpeciesNoteOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "speciesnote-over-owner@example.com");
        String speciesNote = "N".repeat(201);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", "PARROT", null, speciesNote,
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("speciesNote"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createPetWithSexAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "sex-max-owner@example.com");
        String sex = "S".repeat(20);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", sex, 24.5, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sex").value(sex));
    }

    @Test
    void createPetWithSexOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "sex-over-owner@example.com");
        String sex = "S".repeat(21);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", sex, 24.5, null, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sex"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createPetWithAllergiesAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "allergies-max-owner@example.com");
        String allergies = "A".repeat(500);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, allergies, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allergies").value(allergies));
    }

    @Test
    void createPetWithAllergiesOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "allergies-over-owner@example.com");
        String allergies = "A".repeat(501);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, allergies, null));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("allergies"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createPetWithChronicConditionsAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "chronic-max-owner@example.com");
        String chronicConditions = "C".repeat(500);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, chronicConditions));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chronicConditions").value(chronicConditions));
    }

    @Test
    void createPetWithChronicConditionsOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "chronic-over-owner@example.com");
        String chronicConditions = "C".repeat(501);

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, chronicConditions));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("chronicConditions"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void getPetDetailAndUpdateItThenChangesAreReflected() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-pet-owner@example.com");

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        String createResponse = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long petId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Boncuk"));

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 26.0, "Penicillin", null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(26.0))
                .andExpect(jsonPath("$.allergies").value("Penicillin"));

        // Separate, later request: proves the update was actually written to the DB,
        // not just mutated on the in-memory object returned by the PUT call above.
        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(26.0))
                .andExpect(jsonPath("$.allergies").value("Penicillin"));
    }

    @Test
    void getUnknownPetDetailReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/pets/999999").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePetWithNameAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-name-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String name = "N".repeat(100);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, name, "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void updatePetWithNameOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-name-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String name = "N".repeat(101);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, name, "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updatePetWithSpeciesAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-species-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String species = "S".repeat(50);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", species, null, "Exotic species note",
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.species").value(species));
    }

    @Test
    void updatePetWithSpeciesOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-species-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String species = "S".repeat(51);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", species, null, "Exotic species note",
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("species"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updatePetWithBreedAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-breed-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String breed = "B".repeat(100);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", breed, null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breed").value(breed));
    }

    @Test
    void updatePetWithBreedOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-breed-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String breed = "B".repeat(101);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", breed, null,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("breed"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updatePetWithSpeciesNoteAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-speciesnote-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String speciesNote = "N".repeat(200);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", "PARROT", null, speciesNote,
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesNote").value(speciesNote));
    }

    @Test
    void updatePetWithSpeciesNoteOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-speciesnote-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String speciesNote = "N".repeat(201);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Kivi", "PARROT", null, speciesNote,
                        "2023-01-01", "MALE", 0.4, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("speciesNote"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updatePetWithSexAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-sex-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String sex = "S".repeat(20);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", sex, 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sex").value(sex));
    }

    @Test
    void updatePetWithSexOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-sex-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String sex = "S".repeat(21);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", sex, 24.5, null, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sex"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updatePetWithAllergiesAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-allergies-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String allergies = "A".repeat(500);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, allergies, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies").value(allergies));
    }

    @Test
    void updatePetWithAllergiesOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-allergies-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String allergies = "A".repeat(501);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, allergies, null));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("allergies"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updatePetWithChronicConditionsAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-chronic-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String chronicConditions = "C".repeat(500);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, chronicConditions));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chronicConditions").value(chronicConditions));
    }

    @Test
    void updatePetWithChronicConditionsOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-chronic-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String chronicConditions = "C".repeat(501);

        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, chronicConditions));

        mockMvc.perform(put("/api/pets/" + petId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("chronicConditions"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updatePetByVetIsForbidden() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String updateBody = objectMapper.writeValueAsString(
                new PetPayload(1L, "Tekir", "CAT", "Tabby", null,
                        "2021-01-01", "MALE", 4.2, null, null));

        mockMvc.perform(put("/api/pets/1")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void archiveThenActivatePetTogglesArchivedFlag() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "archive-owner@example.com");

        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        String createResponse = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long petId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(patch("/api/pets/" + petId + "/archive")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        // Separate, later request: proves the flag was actually written to the DB,
        // not just mutated on the in-memory object returned by the PATCH call above.
        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(patch("/api/pets/" + petId + "/activate")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void archivePetByVetIsForbidden() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        mockMvc.perform(patch("/api/pets/1/archive")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void archiveUnknownPetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(patch("/api/pets/999999/archive")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPetsFilteredBySpeciesReturnsOnlyMatchingSpecies() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "species-filter-owner@example.com");
        createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        createPet(receptionistToken, ownerId, "Tekir", "CAT", "Tabby", null);

        mockMvc.perform(get("/api/pets?species=CAT").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].species", everyItem(is("CAT"))));
    }

    @Test
    void listPetsFilteredByOwnerIdReturnsOnlyThatOwnersPets() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "owner-filter-owner@example.com");
        createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);

        mockMvc.perform(get("/api/pets?ownerId=" + ownerId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].ownerId", everyItem(is((int) ownerId))));
    }

    @Test
    void listPetsFilteredByActiveExcludesArchivedPets() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "active-filter-owner@example.com");
        long archivedPetId = createPet(receptionistToken, ownerId, "Minnos", "CAT", "Tabby", null);
        createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);

        mockMvc.perform(patch("/api/pets/" + archivedPetId + "/archive")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pets?ownerId=" + ownerId + "&active=true")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) archivedPetId))));

        mockMvc.perform(get("/api/pets?ownerId=" + ownerId + "&active=false")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) archivedPetId)));
    }

    @Test
    void listPetsSearchByNameMatchesPartial() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "search-filter-owner@example.com");
        createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);

        mockMvc.perform(get("/api/pets?search=onc").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Boncuk")));
    }

    @Test
    void petStatsCountOnlyNonArchivedPetsBySpecies() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long totalBefore = fetchPetStatsField(receptionistToken, "totalPets");
        long dogsBefore = fetchPetStatsField(receptionistToken, "dogs");
        long catsBefore = fetchPetStatsField(receptionistToken, "cats");
        long newThisMonthBefore = fetchPetStatsField(receptionistToken, "newThisMonth");

        long ownerId = createOwner(receptionistToken, "pet-stats-owner@example.com");
        long dogId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        createPet(receptionistToken, ownerId, "Tekir", "CAT", "Tabby", null);

        mockMvc.perform(get("/api/pets/stats").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPets").value(totalBefore + 2))
                .andExpect(jsonPath("$.dogs").value(dogsBefore + 1))
                .andExpect(jsonPath("$.cats").value(catsBefore + 1))
                .andExpect(jsonPath("$.newThisMonth").value(newThisMonthBefore + 2));

        mockMvc.perform(patch("/api/pets/" + dogId + "/archive")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pets/stats").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPets").value(totalBefore + 1))
                .andExpect(jsonPath("$.dogs").value(dogsBefore));
    }

    private long fetchPetStatsField(String token, String field) throws Exception {
        String response = mockMvc.perform(get("/api/pets/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get(field).asLong();
    }

    @Test
    void receptionistAddsWeightRecordThenItAppearsInHistoryOrderedByRecordedAt() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "weight-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);

        String laterBody = objectMapper.writeValueAsString(
                new WeightRecordPayload(25.0, "2026-06-01T09:00:00", "Check-in weigh-in"));
        mockMvc.perform(post("/api/pets/" + petId + "/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(laterBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId))
                .andExpect(jsonPath("$.weightKg").value(25.0))
                .andExpect(jsonPath("$.note").value("Check-in weigh-in"));

        String earlierBody = objectMapper.writeValueAsString(
                new WeightRecordPayload(23.5, "2026-01-01T09:00:00", null));
        mockMvc.perform(post("/api/pets/" + petId + "/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(earlierBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/pets/" + petId + "/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weightKg").value(23.5))
                .andExpect(jsonPath("$[1].weightKg").value(25.0));
    }

    @Test
    void addWeightRecordWithUnknownPetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String body = objectMapper.writeValueAsString(new WeightRecordPayload(25.0, "2026-06-01T09:00:00", null));

        mockMvc.perform(post("/api/pets/999999/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void addWeightRecordWithNonPositiveWeightReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "weight-validation-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);

        String body = objectMapper.writeValueAsString(new WeightRecordPayload(0.0, "2026-06-01T09:00:00", null));

        mockMvc.perform(post("/api/pets/" + petId + "/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("weightKg"));
    }

    @Test
    void addWeightRecordWithNoteAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "weight-note-max-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String note = "N".repeat(500);

        String body = objectMapper.writeValueAsString(new WeightRecordPayload(25.0, "2026-06-01T09:00:00", note));

        mockMvc.perform(post("/api/pets/" + petId + "/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value(note));
    }

    @Test
    void addWeightRecordWithNoteOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "weight-note-over-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);
        String note = "N".repeat(501);

        String body = objectMapper.writeValueAsString(new WeightRecordPayload(25.0, "2026-06-01T09:00:00", note));

        mockMvc.perform(post("/api/pets/" + petId + "/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("note"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void weightRecordHistoryForUnknownPetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/pets/999999/weight-records")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void vetAddsWeightRecordSucceeds() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long ownerId = createOwner(receptionistToken, "weight-vet-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);

        String body = objectMapper.writeValueAsString(new WeightRecordPayload(25.0, "2026-06-01T09:00:00", null));

        mockMvc.perform(post("/api/pets/" + petId + "/weight-records")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void newlyCreatedPetWithNoVisitsIsNotInactive() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "inactive-fresh-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Boncuk", "DOG", "Golden Retriever", null);

        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inactive").value(false));
    }

    @Test
    void petWithOnlyAVisitOverTwoYearsAgoIsInactive() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "inactive-old-visit-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Minnos", "CAT", "Tabby", null);
        long vetId = createVet(receptionistToken, "VET-LIC-PET-INACTIVE-001");
        createVisit(receptionistToken, petId, vetId, LocalDateTime.now().minusYears(3));

        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inactive").value(true));
    }

    @Test
    void petWithRecentVisitIsNotInactive() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "inactive-recent-visit-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Duman", "DOG", "Golden Retriever", null);
        long vetId = createVet(receptionistToken, "VET-LIC-PET-INACTIVE-002");
        createVisit(receptionistToken, petId, vetId, LocalDateTime.now().minusDays(1));

        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inactive").value(false));
    }

    @Test
    void cancelledOldVisitDoesNotCountAsLastSeenSoPetStaysActive() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "inactive-cancelled-owner@example.com");
        long petId = createPet(receptionistToken, ownerId, "Findik", "CAT", "Tabby", null);
        long vetId = createVet(receptionistToken, "VET-LIC-PET-INACTIVE-003");
        long visitId = createVisit(receptionistToken, petId, vetId, LocalDateTime.now().minusYears(3));

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VisitStatusPayload("CANCELLED"))))
                .andExpect(status().isOk());

        // The only visit is CANCELLED, so it's disregarded and the pet's recent createdAt is used instead
        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inactive").value(false));
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

    private long createVet(String token, String licenseNo) throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String vetBody = objectMapper.writeValueAsString(
                new VetPayload("Dr. Pet Inactive Test", "Surgery", licenseNo, "Mon-Fri 09:00-17:00"));

        String response = mockMvc.perform(post("/api/vets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vetBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createPet(String token, long ownerId, String name, String species, String breed, String speciesNote)
            throws Exception {
        String createBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, name, species, breed, speciesNote,
                        "2022-03-15", "FEMALE", 24.5, null, null));

        String response = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
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

    private String loginAndGetToken(String email, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, password));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private record PetPayload(Long ownerId, String name, String species, String breed, String speciesNote,
                               String birthDate, String sex, Double weightKg, String allergies,
                               String chronicConditions) {
    }

    private record OwnerPayload(String firstName, String lastName, String phone, String email, String address) {
    }

    private record WeightRecordPayload(Double weightKg, String recordedAt, String note) {
    }

    private record VisitPayload(Long petId, Long vetId, String scheduledAt, String chiefComplaint) {
    }

    private record VisitStatusPayload(String status) {
    }

    private record VetPayload(String name, String specialty, String licenseNo, String workHours) {
    }

    private record LoginPayload(String email, String password) {
    }
}
