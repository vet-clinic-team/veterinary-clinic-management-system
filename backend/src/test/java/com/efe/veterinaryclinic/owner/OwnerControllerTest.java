package com.efe.veterinaryclinic.owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerControllerTest {

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
    void receptionistCreatesOwnerThenOwnerAppearsInList() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", "mehmet.demir@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Mehmet"))
                .andExpect(jsonPath("$.lastName").value("Demir"))
                .andExpect(jsonPath("$.petCount").value(0))
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(get("/api/owners").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void searchOwnersByLastNameFiltersToMatchingOwners() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String uniqueLastName = "Ozkanli" + System.nanoTime();
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Elif", uniqueLastName, "+90 555 222 3344",
                        "elif." + System.nanoTime() + "@example.com", "Izmir, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/owners")
                        .param("search", uniqueLastName.toLowerCase())
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value(uniqueLastName));
    }

    @Test
    void createOwnerByVetIsForbidden() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Ayse", "Yilmaz", "+90 555 000 0000", "ayse.yilmaz@example.com", "Ankara, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOwnerWithBlankFirstNameReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("", "Demir", "+90 555 123 4567", "mehmet.demir2@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"));
    }

    @Test
    void createOwnerWithFirstNameAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String firstName = "A".repeat(100);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload(firstName, "Demir", "+90 555 123 4567", "max-firstname@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value(firstName));
    }

    @Test
    void createOwnerWithFirstNameOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String firstName = "A".repeat(101);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload(firstName, "Demir", "+90 555 123 4567", "over-firstname@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createOwnerWithLastNameAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String lastName = "B".repeat(100);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", lastName, "+90 555 123 4567", "max-lastname@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastName").value(lastName));
    }

    @Test
    void createOwnerWithLastNameOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String lastName = "B".repeat(101);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", lastName, "+90 555 123 4567", "over-lastname@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("lastName"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createOwnerWithPhoneAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String phone = "1".repeat(20);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", phone, "max-phone@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value(phone));
    }

    @Test
    void createOwnerWithPhoneOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String phone = "1".repeat(21);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", phone, "over-phone@example.com", "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createOwnerWithEmailAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String localPart = "a".repeat(64);
        String domainLabel = "a".repeat(61);
        String email = localPart + "@" + domainLabel + "." + domainLabel + "." + domainLabel + ".com";
        assertThat(email.length()).isEqualTo(254);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", email, "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void createOwnerWithEmailOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String localPart = "a".repeat(64);
        String domainLabel = "a".repeat(61);
        String domainLabelPlusOne = "a".repeat(62);
        String email = localPart + "@" + domainLabelPlusOne + "." + domainLabel + "." + domainLabel + ".com";
        assertThat(email.length()).isEqualTo(255);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", email, "Istanbul, Turkey"));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void createOwnerWithAddressAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String address = "C".repeat(500);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", "max-address@example.com", address));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address").value(address));
    }

    @Test
    void createOwnerWithAddressOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String address = "C".repeat(501);
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", "over-address@example.com", address));

        mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("address"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void updateOwnerWithAddressAtMaxLengthIsAccepted() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-max-address@example.com");
        String address = "D".repeat(500);

        String updateBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", "update-max-address@example.com", address));

        mockMvc.perform(put("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(address));
    }

    @Test
    void updateOwnerWithAddressOverMaxLengthReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "update-over-address@example.com");
        String address = "D".repeat(501);

        String updateBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", "update-over-address@example.com", address));

        mockMvc.perform(put("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("address"))
                .andExpect(jsonPath("$.message").value(not(containsStringIgnoringCase("sql"))));
    }

    @Test
    void receptionistUpdatesOwnerFields() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "before-update@example.com");

        String updateBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir-Updated", "+90 555 999 9999", "after-update@example.com", "Ankara, Turkey"));

        mockMvc.perform(put("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Demir-Updated"))
                .andExpect(jsonPath("$.email").value("after-update@example.com"));

        // Separate, later request: proves the update was actually written to the DB,
        // not just mutated on the in-memory object returned by the PUT call above.
        mockMvc.perform(get("/api/owners/" + ownerId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Demir-Updated"))
                .andExpect(jsonPath("$.email").value("after-update@example.com"));
    }

    @Test
    void updateUnknownOwnerReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String updateBody = objectMapper.writeValueAsString(
                new OwnerPayload("Mehmet", "Demir", "+90 555 123 4567", "unknown-owner@example.com", "Istanbul, Turkey"));

        mockMvc.perform(put("/api/owners/999999")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOwnerNotArchivedReturnsConflict() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "not-archived-owner@example.com");

        mockMvc.perform(delete("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Owner must be archived before it can be deleted"));
    }

    @Test
    void adminDeletesArchivedOwnerWithNoPets() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "no-pets-owner@example.com");

        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(delete("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteArchivedOwnerWithPetsReturnsConflict() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "owner-with-pet@example.com");

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        String petResponse = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long petId = objectMapper.readTree(petResponse).get("id").asLong();

        mockMvc.perform(patch("/api/pets/" + petId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(delete("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Owner has 1 pet(s) and cannot be deleted"));
    }

    @Test
    void archiveThenActivateOwnerTogglesArchivedFlag() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "toggle-owner@example.com");

        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/owners/" + ownerId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(patch("/api/owners/" + ownerId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/api/owners/" + ownerId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void archiveOwnerWithActivePetReturnsConflict() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "active-pet-owner@example.com");

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Owner has active pet(s) and cannot be archived"));
    }

    @Test
    void archiveOwnerByVetIsForbidden() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long ownerId = createOwner(adminToken, "vet-archive-owner@example.com");

        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void archiveUnknownOwnerReturnsNotFound() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        mockMvc.perform(patch("/api/owners/999999/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void activatingPetAutoActivatesArchivedOwner() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "auto-activate-owner@example.com");

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        String petResponse = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long petId = objectMapper.readTree(petResponse).get("id").asLong();

        mockMvc.perform(patch("/api/pets/" + petId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(patch("/api/pets/" + petId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/owners/" + ownerId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void creatingPetForArchivedOwnerAutoActivatesOwner() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "auto-activate-on-create@example.com");

        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Pamuk", "CAT", "Persian", null,
                        "2023-01-10", "MALE", 4.2, null, null));
        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/owners/" + ownerId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void listOwnersFilteredByActiveExcludesArchivedOwners() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String uniqueLastName = "Arsivli" + System.nanoTime();
        String createBody = objectMapper.writeValueAsString(
                new OwnerPayload("Kemal", uniqueLastName, "+90 555 444 5566",
                        "list-filter-" + System.nanoTime() + "@example.com", "Bursa, Turkey"));
        String createResponse = mockMvc.perform(post("/api/owners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ownerId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(patch("/api/owners/" + ownerId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/owners")
                        .param("active", "true")
                        .param("search", uniqueLastName)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/owners")
                        .param("active", "false")
                        .param("search", uniqueLastName)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownerId));
    }

    @Test
    void deleteOwnerByReceptionistIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long ownerId = createOwner(receptionistToken, "forbidden-delete-owner@example.com");

        mockMvc.perform(delete("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOwnerDetailIncludesPetsAndCount() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "owner-detail@example.com");

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownerId))
                .andExpect(jsonPath("$.petCount").value(1))
                .andExpect(jsonPath("$.pets[0].name").value("Boncuk"));
    }

    @Test
    void getOwnerDetailIncludesInvoicesForOwnersVisits() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long ownerId = createOwner(adminToken, "owner-detail-invoices@example.com");

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        String petResponse = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long petId = objectMapper.readTree(petResponse).get("id").asLong();

        String vetBody = objectMapper.writeValueAsString(
                new VetPayload("Dr. Owner Detail Test", "Surgery", "VET-LIC-OWNER-DETAIL-001", "Mon-Fri 09:00-17:00"));
        String vetResponse = mockMvc.perform(post("/api/vets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vetBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long vetId = objectMapper.readTree(vetResponse).get("id").asLong();

        String visitBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-10T14:30:00", "Annual checkup"));
        String visitResponse = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long visitId = objectMapper.readTree(visitResponse).get("id").asLong();

        String invoiceBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));
        String invoiceResponse = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long invoiceId = objectMapper.readTree(invoiceResponse).get("id").asLong();

        mockMvc.perform(get("/api/owners/" + ownerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoices.length()").value(1))
                .andExpect(jsonPath("$.invoices[0].id").value(invoiceId))
                .andExpect(jsonPath("$.invoices[0].visitId").value(visitId))
                .andExpect(jsonPath("$.invoices[0].total").value(590.00));
    }

    @Test
    void getUnknownOwnerDetailReturnsNotFound() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        mockMvc.perform(get("/api/owners/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerStatsReflectTotalsAfterCreatingOwnerAndPet() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long totalOwnersBefore = fetchStatsField(receptionistToken, "totalOwners");
        long totalPetsBefore = fetchStatsField(receptionistToken, "totalPets");
        long newThisMonthBefore = fetchStatsField(receptionistToken, "newOwnersThisMonth");

        long ownerId = createOwner(receptionistToken, "stats-owner@example.com");
        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, "Boncuk", "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, null, null));
        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/owners/stats").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOwners").value(totalOwnersBefore + 1))
                .andExpect(jsonPath("$.totalPets").value(totalPetsBefore + 1))
                .andExpect(jsonPath("$.newOwnersThisMonth").value(newThisMonthBefore + 1));
    }

    private long fetchStatsField(String token, String field) throws Exception {
        String response = mockMvc.perform(get("/api/owners/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get(field).asLong();
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

    private record InvoicePayload(Long visitId, List<InvoiceItemPayload> items) {
    }

    private record InvoiceItemPayload(String description, String category, Integer quantity, BigDecimal unitPrice) {
    }

    private record LoginPayload(String email, String password) {
    }
}
