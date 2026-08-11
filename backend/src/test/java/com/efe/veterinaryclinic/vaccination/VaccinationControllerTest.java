package com.efe.veterinaryclinic.vaccination;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VaccinationControllerTest {

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
    void vetCreatesVaccinationThenVaccinationCanBeFetchedById() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-owner@example.com", "Boncuk");

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-2026-778", "Dr. Ahmet Kaya"));

        String response = mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId))
                .andExpect(jsonPath("$.vaccineType").value("ONE_YEAR"))
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-778"))
                .andExpect(jsonPath("$.administeredBy").value("Dr. Ahmet Kaya"))
                .andExpect(jsonPath("$.nextDueDate").value("2027-07-04"))
                .andReturn().getResponse().getContentAsString();

        long vaccinationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/vaccinations/" + vaccinationId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vaccinationId))
                .andExpect(jsonPath("$.petId").value(petId));
    }

    @Test
    void createThreeYearVaccinationCalculatesThreeYearNextDueDate() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-three-year@example.com", "Duman");

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "THREE_YEAR", "2026-07-04T09:00:00", "LOT-2026-800", "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextDueDate").value("2029-07-04"));
    }

    @Test
    void createVaccinationWithUnlistedTypeDefaultsToOneYearNextDueDate() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-unlisted@example.com", "Cesur");

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "Some Other Vaccine", "2026-07-04T09:00:00", "LOT-2026-801", "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextDueDate").value("2027-07-04"));
    }

    @Test
    void updatingAdministeredAtRecalculatesNextDueDate() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-recalc@example.com", "Nala");
        long vaccinationId = createVaccination(vetToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-802", "Dr. Kaya");

        String updateBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-08-01T09:00:00", "LOT-802", "Dr. Kaya"));

        mockMvc.perform(put("/api/vaccinations/" + vaccinationId)
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextDueDate").value("2027-08-01"));
    }

    @Test
    void createVaccinationByReceptionistIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-forbidden@example.com", "Minnos");

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-2026-779", "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createVaccinationWithBlankVaccineTypeReturnsValidationError() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-validation@example.com", "Tekir");

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "", "2026-07-04T09:00:00", "LOT-2026-780", "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("vaccineType"));
    }

    @Test
    void createVaccinationWithVaccineTypeAtMaxLengthSucceeds() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-type-max@example.com", "Max Type");
        String vaccineType = "A".repeat(100);

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, vaccineType, "2026-07-04T09:00:00", "LOT-MAX-1", "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vaccineType").value(vaccineType));
    }

    @Test
    void createVaccinationWithVaccineTypeOverMaxLengthReturnsValidationError() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-type-over@example.com", "Over Type");
        String vaccineType = "A".repeat(101);

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, vaccineType, "2026-07-04T09:00:00", "LOT-MAX-2", "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("vaccineType"));
    }

    @Test
    void createVaccinationWithLotNumberAtMaxLengthSucceeds() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-lot-max@example.com", "Max Lot");
        String lotNumber = "L".repeat(100);

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", lotNumber, "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotNumber").value(lotNumber));
    }

    @Test
    void createVaccinationWithLotNumberOverMaxLengthReturnsValidationError() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-lot-over@example.com", "Over Lot");
        String lotNumber = "L".repeat(101);

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", lotNumber, "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("lotNumber"));
    }

    @Test
    void createVaccinationWithAdministeredByAtMaxLengthSucceeds() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-by-max@example.com", "Max By");
        String administeredBy = "D".repeat(100);

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-MAX-3", administeredBy));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.administeredBy").value(administeredBy));
    }

    @Test
    void createVaccinationWithAdministeredByOverMaxLengthReturnsValidationError() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-by-over@example.com", "Over By");
        String administeredBy = "D".repeat(101);

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-MAX-4", administeredBy));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("administeredBy"));
    }

    @Test
    void updateVaccinationWithVaccineTypeOverMaxLengthReturnsValidationError() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-update-type-over@example.com", "Update Over Type");
        long vaccinationId = createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-U1", "Dr. Kaya");

        String updateBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "A".repeat(101), "2026-07-04T09:00:00", "LOT-U1", "Dr. Kaya"));

        mockMvc.perform(put("/api/vaccinations/" + vaccinationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("vaccineType"));
    }

    @Test
    void updateVaccinationWithLotNumberOverMaxLengthReturnsValidationError() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-update-lot-over@example.com", "Update Over Lot");
        long vaccinationId = createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-U2", "Dr. Kaya");

        String updateBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "L".repeat(101), "Dr. Kaya"));

        mockMvc.perform(put("/api/vaccinations/" + vaccinationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("lotNumber"));
    }

    @Test
    void updateVaccinationWithAdministeredByOverMaxLengthReturnsValidationError() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-update-by-over@example.com", "Update Over By");
        long vaccinationId = createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-U3", "Dr. Kaya");

        String updateBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-U3", "D".repeat(101)));

        mockMvc.perform(put("/api/vaccinations/" + vaccinationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("administeredBy"));
    }

    @Test
    void createVaccinationWithOversizedFieldDoesNotLeakDatabaseDetails() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-leak@example.com", "Leak Check");

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "A".repeat(300), "2026-07-04T09:00:00", "LOT-LEAK", "Dr. Ahmet Kaya"));

        String response = mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(response.toLowerCase())
                .doesNotContain("sql", "column", "varchar", "psqlexception");
    }

    @Test
    void createVaccinationWithUnknownPetReturnsNotFound() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(999999L, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-2026-781", "Dr. Ahmet Kaya"));

        mockMvc.perform(post("/api/vaccinations")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminUpdatesVaccinationFields() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-update@example.com", "Karabas");
        long vaccinationId = createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-A", "Dr. Kaya");

        String updateBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "THREE_YEAR", "2026-07-05T10:00:00", "LOT-B", "Dr. Yilmaz"));

        mockMvc.perform(put("/api/vaccinations/" + vaccinationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vaccineType").value("THREE_YEAR"))
                .andExpect(jsonPath("$.lotNumber").value("LOT-B"));
    }

    @Test
    void updateVaccinationByReceptionistIsForbidden() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-update-forbidden@example.com", "Pamuk");
        long vaccinationId = createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-C", "Dr. Kaya");

        String updateBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-C", "Hacked"));

        mockMvc.perform(put("/api/vaccinations/" + vaccinationId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUnknownVaccinationReturnsNotFound() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-update-unknown@example.com", "Zeytin");

        String updateBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-D", "Dr. Kaya"));

        mockMvc.perform(put("/api/vaccinations/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUnknownVaccinationReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/vaccinations/999999").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDeletesVaccinationThenGetByIdReturnsNotFound() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-delete@example.com", "Sansar");
        long vaccinationId = createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-E", "Dr. Kaya");

        mockMvc.perform(delete("/api/vaccinations/" + vaccinationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vaccinations/" + vaccinationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteVaccinationByVetIsForbidden() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-delete-forbidden@example.com", "Findik");
        long vaccinationId = createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-F", "Dr. Kaya");

        mockMvc.perform(delete("/api/vaccinations/" + vaccinationId).header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listVaccinationsReturnsPaginatedResults() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "vaccination-list@example.com", "Leo");
        createVaccination(adminToken, petId, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-G", "Dr. Kaya");

        mockMvc.perform(get("/api/vaccinations").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void petVaccinationHistoryReturnsOnlyThatPetsVaccinations() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petIdA = createPet(receptionistToken, "vaccination-history-a@example.com", "Fistik");
        long petIdB = createPet(receptionistToken, "vaccination-history-b@example.com", "Cesur");
        long vaccinationForPetA = createVaccination(adminToken, petIdA, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-H", "Dr. Kaya");
        createVaccination(adminToken, petIdB, "ONE_YEAR", "2026-07-04T09:00:00", "LOT-I", "Dr. Kaya");

        mockMvc.perform(get("/api/pets/" + petIdA + "/vaccinations")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(vaccinationForPetA))
                .andExpect(jsonPath("$.content[0].petId").value(petIdA));
    }

    @Test
    void petVaccinationHistoryForUnknownPetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/pets/999999/vaccinations")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void vaccinationStatsReflectTotalsAdministeredTodayUpcomingDueAndUniquePetsAfterCreatingOne() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long totalBefore = fetchVaccinationStatsField(receptionistToken, "totalVaccinations");
        long administeredTodayBefore = fetchVaccinationStatsField(receptionistToken, "administeredToday");
        long upcomingDueBefore = fetchVaccinationStatsField(receptionistToken, "upcomingDue");
        long vaccinatedPetsBefore = fetchVaccinationStatsField(receptionistToken, "vaccinatedPets");

        long petId = createPet(receptionistToken, "vaccination-stats@example.com", "Stats Kivi");
        String administeredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        createVaccination(vetToken, petId, "ONE_YEAR", administeredAt, "LOT-STATS-001", "Dr. Ahmet Kaya");

        mockMvc.perform(get("/api/vaccinations/stats").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVaccinations").value(totalBefore + 1))
                .andExpect(jsonPath("$.administeredToday").value(administeredTodayBefore + 1))
                .andExpect(jsonPath("$.upcomingDue").value(upcomingDueBefore + 1))
                .andExpect(jsonPath("$.vaccinatedPets").value(vaccinatedPetsBefore + 1));
    }

    private long fetchVaccinationStatsField(String token, String field) throws Exception {
        String response = mockMvc.perform(get("/api/vaccinations/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get(field).asLong();
    }

    private long createVaccination(String token, long petId, String vaccineType, String administeredAt,
                                    String lotNumber, String administeredBy) throws Exception {
        String createBody = objectMapper.writeValueAsString(
                new VaccinationPayload(petId, vaccineType, administeredAt, lotNumber, administeredBy));

        String response = mockMvc.perform(post("/api/vaccinations")
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

    private String loginAndGetToken(String email, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, password));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private record VaccinationPayload(Long petId, String vaccineType, String administeredAt, String lotNumber,
                                       String administeredBy) {
    }

    private record PetPayload(Long ownerId, String name, String species, String breed, String speciesNote,
                               String birthDate, String sex, Double weightKg, String allergies,
                               String chronicConditions) {
    }

    private record OwnerPayload(String firstName, String lastName, String phone, String email, String address) {
    }

    private record LoginPayload(String email, String password) {
    }
}
