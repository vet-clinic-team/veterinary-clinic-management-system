package com.efe.veterinaryclinic.visit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VisitControllerTest {

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
    void receptionistCreatesVisitThenVisitCanBeFetchedById() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-owner@example.com", "Boncuk");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-001");

        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-10T14:30:00", "Limping on front left leg"));

        String response = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId))
                .andExpect(jsonPath("$.vetId").value(vetId))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.chiefComplaint").value("Limping on front left leg"))
                .andReturn().getResponse().getContentAsString();

        long visitId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/visits/" + visitId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visitId))
                .andExpect(jsonPath("$.petId").value(petId))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void createVisitByVetIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-vet-forbidden@example.com", "Minnos");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-002");

        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-10T15:00:00", "Annual checkup"));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createVisitWithBlankChiefComplaintReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-validation@example.com", "Tekir");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-003");

        String createBody = objectMapper.writeValueAsString(new VisitPayload(petId, vetId, "2026-07-10T15:30:00", ""));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("chiefComplaint"));
    }

    @Test
    void createVisitWithChiefComplaintAtMaxLengthSucceeds() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-complaint-max@example.com", "Max Complaint");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-030");
        String chiefComplaint = "A".repeat(500);

        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-10T15:30:00", chiefComplaint));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chiefComplaint").value(chiefComplaint));
    }

    @Test
    void createVisitWithChiefComplaintOverMaxLengthReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-complaint-over@example.com", "Over Complaint");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-031");
        String chiefComplaint = "A".repeat(501);

        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-10T15:30:00", chiefComplaint));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("chiefComplaint"));
    }

    @Test
    void createVisitWithUnknownPetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-004");

        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(999999L, vetId, "2026-07-10T16:00:00", "Checkup"));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void createVisitWithUnknownVetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-unknown-vet@example.com", "Duman");

        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, 999999L, "2026-07-10T16:30:00", "Checkup"));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionistUpdatesVisitFields() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-update@example.com", "Karabas");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-005");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-07-10T17:00:00", "Initial complaint");

        String updateBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-11T09:00:00", "Updated complaint"));

        mockMvc.perform(put("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chiefComplaint").value("Updated complaint"))
                .andExpect(jsonPath("$.scheduledAt").value("2026-07-11T09:00:00"));

        mockMvc.perform(get("/api/visits/" + visitId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chiefComplaint").value("Updated complaint"));
    }

    @Test
    void updateVisitWithChiefComplaintOverMaxLengthReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-update-complaint-over@example.com", "Update Over Complaint");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-032");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-07-10T17:00:00", "Initial complaint");

        String updateBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-11T09:00:00", "A".repeat(501)));

        mockMvc.perform(put("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("chiefComplaint"));
    }

    @Test
    void updateVisitByVetIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-update-forbidden@example.com", "Pamuk");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-006");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-07-10T18:00:00", "Initial complaint");

        String updateBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-11T10:00:00", "Hacked complaint"));

        mockMvc.perform(put("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUnknownVisitReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-update-unknown@example.com", "Zeytin");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-007");

        String updateBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-11T11:00:00", "Checkup"));

        mockMvc.perform(put("/api/visits/999999")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUnknownVisitReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/visits/999999").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void vetUpdatesVisitStatus() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-status@example.com", "Sansar");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-008");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-07-10T19:00:00", "Initial complaint");

        String statusBody = objectMapper.writeValueAsString(new VisitStatusPayload("CHECKED_IN"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        mockMvc.perform(get("/api/visits/" + visitId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
    }

    @Test
    void updateStatusWithInvalidValueReturnsBadRequest() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-status-invalid@example.com", "Findik");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-009");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-07-10T19:30:00", "Initial complaint");

        String statusBody = "{\"status\":\"NOT_A_REAL_STATUS\"}";

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatusForUnknownVisitReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String statusBody = objectMapper.writeValueAsString(new VisitStatusPayload("CHECKED_IN"));

        mockMvc.perform(patch("/api/visits/999999/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void vetUpdatesMedicalNotes() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-medical-notes@example.com", "Şeker");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-021");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-01T10:00:00", "Limping");

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("Mild sprain", "Rest for 1 week, anti-inflammatory prescribed", "2026-12-08"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Mild sprain"))
                .andExpect(jsonPath("$.treatmentNotes").value("Rest for 1 week, anti-inflammatory prescribed"))
                .andExpect(jsonPath("$.followUpDate").value("2026-12-08"));

        mockMvc.perform(get("/api/visits/" + visitId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Mild sprain"));
    }

    @Test
    void updateMedicalNotesWithDiagnosisAtMaxLengthSucceeds() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-diagnosis-max@example.com", "Max Diagnosis");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-033");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-01T10:00:00", "Limping");
        String diagnosis = "D".repeat(2000);

        String notesBody = objectMapper.writeValueAsString(new MedicalNotesPayload(diagnosis, "Rest", "2026-12-08"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value(diagnosis));
    }

    @Test
    void updateMedicalNotesWithDiagnosisOverMaxLengthReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-diagnosis-over@example.com", "Over Diagnosis");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-034");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-01T10:00:00", "Limping");

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("D".repeat(2001), "Rest", "2026-12-08"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("diagnosis"));
    }

    @Test
    void updateMedicalNotesWithTreatmentNotesAtMaxLengthSucceeds() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-treatment-max@example.com", "Max Treatment");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-035");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-01T10:00:00", "Limping");
        String treatmentNotes = "T".repeat(4000);

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("Mild sprain", treatmentNotes, "2026-12-08"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treatmentNotes").value(treatmentNotes));
    }

    @Test
    void updateMedicalNotesWithTreatmentNotesOverMaxLengthReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-treatment-over@example.com", "Over Treatment");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-036");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-01T10:00:00", "Limping");

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("Mild sprain", "T".repeat(4001), "2026-12-08"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("treatmentNotes"));
    }

    @Test
    void updateMedicalNotesWithOversizedFieldDoesNotLeakDatabaseDetails() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-notes-leak@example.com", "Leak Check");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-037");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-01T10:00:00", "Limping");

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("D".repeat(5000), "Rest", "2026-12-08"));

        String response = mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(response.toLowerCase())
                .doesNotContain("sql", "column", "varchar", "psqlexception");
    }

    @Test
    void updateMedicalNotesByReceptionistIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-medical-notes-forbidden@example.com", "Duman");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-022");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-02T10:00:00", "Limping");

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("Mild sprain", "Rest", "2026-12-09"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicalNotesReferencingPetAllergyReturnsWarning() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPetWithAllergies(receptionistToken, "visit-allergy-warning@example.com", "Fıstık", "Penicillin");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-023");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-03T10:00:00", "Ear infection");

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("Ear infection", "Prescribed Penicillin for 7 days", "2026-12-10"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0]").value("Pet is recorded as allergic to Penicillin"));
    }

    @Test
    void medicalNotesNotReferencingPetAllergyReturnsNoWarning() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPetWithAllergies(receptionistToken, "visit-no-allergy-warning@example.com", "Boncuk", "Penicillin");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-024");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-04T10:00:00", "Annual checkup");

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("Healthy", "Prescribed Amoxicillin", "2026-12-11"));

        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings.length()").value(0));
    }

    @Test
    void updateMedicalNotesForUnknownVisitReturnsNotFound() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        String notesBody = objectMapper.writeValueAsString(
                new MedicalNotesPayload("Mild sprain", "Rest", "2026-12-09"));

        mockMvc.perform(patch("/api/visits/999999/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notesBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void vetCreatesFollowUpVisitForCompletedVisitWithFollowUpDate() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-followup@example.com", "Mira");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-025");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-05T10:00:00", "Limping");

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VisitStatusPayload("COMPLETED"))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MedicalNotesPayload("Mild sprain", "Rest", "2026-12-20"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/visits/" + visitId + "/follow-up")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(petId))
                .andExpect(jsonPath("$.vetId").value(vetId))
                .andExpect(jsonPath("$.scheduledAt").value("2026-12-20T09:00:00"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.chiefComplaint").value("Follow-up visit"));
    }

    @Test
    void followUpByReceptionistIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-followup-forbidden@example.com", "Roman");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-026");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-06T10:00:00", "Limping");

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VisitStatusPayload("COMPLETED"))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MedicalNotesPayload("Mild sprain", "Rest", "2026-12-21"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/visits/" + visitId + "/follow-up")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void followUpForNonCompletedVisitReturnsConflict() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-followup-notcompleted@example.com", "Toprak");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-027");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-07T10:00:00", "Limping");

        mockMvc.perform(post("/api/visits/" + visitId + "/follow-up")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isConflict());
    }

    @Test
    void followUpForVisitWithoutFollowUpDateReturnsConflict() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-followup-nodate@example.com", "Ceviz");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-028");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-08T10:00:00", "Limping");

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VisitStatusPayload("COMPLETED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/visits/" + visitId + "/follow-up")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isConflict());
    }

    @Test
    void followUpConflictingWithExistingAppointmentReturnsConflict() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long petId = createPet(receptionistToken, "visit-followup-overlap@example.com", "Ada");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-029");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-12-09T10:00:00", "Limping");

        mockMvc.perform(patch("/api/visits/" + visitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VisitStatusPayload("COMPLETED"))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/visits/" + visitId + "/medical-notes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MedicalNotesPayload("Mild sprain", "Rest", "2026-12-22"))))
                .andExpect(status().isOk());
        createVisit(receptionistToken, petId, vetId, "2026-12-22T09:00:00", "Already booked at follow-up time");

        mockMvc.perform(post("/api/visits/" + visitId + "/follow-up")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isConflict());
    }

    @Test
    void followUpForUnknownVisitReturnsNotFound() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        mockMvc.perform(post("/api/visits/999999/follow-up")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void listVisitsFilteredByVetIdReturnsOnlyThatVetsVisits() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-list-vet@example.com", "Leo");
        long vetIdA = createVet(receptionistToken, "VET-LIC-VISIT-010");
        long vetIdB = createVet(receptionistToken, "VET-LIC-VISIT-011");
        createVisit(receptionistToken, petId, vetIdA, "2026-08-01T10:00:00", "Vet A complaint");
        createVisit(receptionistToken, petId, vetIdB, "2026-08-01T11:00:00", "Vet B complaint");

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("vetId", String.valueOf(vetIdA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].vetId").value(vetIdA));
    }

    @Test
    void listVisitsFilteredByStatusReturnsOnlyMatchingVisits() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-list-status@example.com", "Duman");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-012");
        long scheduledVisitId = createVisit(receptionistToken, petId, vetId, "2026-08-02T09:00:00", "Needs checkin");
        long toCheckIn = createVisit(receptionistToken, petId, vetId, "2026-08-02T10:00:00", "Will be checked in");

        String statusBody = objectMapper.writeValueAsString(new VisitStatusPayload("CHECKED_IN"));
        mockMvc.perform(patch("/api/visits/" + toCheckIn + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("status", "CHECKED_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + toCheckIn + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + scheduledVisitId + ")]").doesNotExist());
    }

    @Test
    void listVisitsFilteredByDateRangeExcludesVisitsOutsideRange() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-list-range@example.com", "Coco");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-013");
        long insideRange = createVisit(receptionistToken, petId, vetId, "2026-09-05T10:00:00", "Inside range");
        long outsideRange = createVisit(receptionistToken, petId, vetId, "2026-09-20T10:00:00", "Outside range");

        mockMvc.perform(get("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + insideRange + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + outsideRange + ")]").doesNotExist());
    }

    @Test
    void petVisitHistoryReturnsOnlyThatPetsVisits() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petIdA = createPet(receptionistToken, "visit-history-a@example.com", "Fistik");
        long petIdB = createPet(receptionistToken, "visit-history-b@example.com", "Cesur");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-020");
        long visitForPetA = createVisit(receptionistToken, petIdA, vetId, "2026-09-15T10:00:00", "Pet A complaint");
        createVisit(receptionistToken, petIdB, vetId, "2026-09-15T11:00:00", "Pet B complaint");

        mockMvc.perform(get("/api/pets/" + petIdA + "/visits")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(visitForPetA))
                .andExpect(jsonPath("$.content[0].petId").value(petIdA));
    }

    @Test
    void petVisitHistoryForUnknownPetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/pets/999999/visits")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void calendarReturnsUnpaginatedVisitsSortedByScheduledAt() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-calendar@example.com", "Şeker");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-014");
        long laterVisit = createVisit(receptionistToken, petId, vetId, "2026-10-05T15:00:00", "Later visit");
        long earlierVisit = createVisit(receptionistToken, petId, vetId, "2026-10-05T09:00:00", "Earlier visit");

        mockMvc.perform(get("/api/visits/calendar")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("vetId", String.valueOf(vetId))
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(earlierVisit))
                .andExpect(jsonPath("$[1].id").value(laterVisit));
    }

    @Test
    void createVisitWithinFifteenMinutesOfExistingVisitForSameVetReturnsConflict() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-overlap@example.com", "Bella");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-015");
        createVisit(receptionistToken, petId, vetId, "2026-11-02T10:00:00", "First appointment");

        String conflictingBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-11-02T10:10:00", "Overlapping appointment"));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictingBody))
                .andExpect(status().isConflict());
    }

    @Test
    void createVisitOutsideFifteenMinuteWindowForSameVetSucceeds() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-no-overlap@example.com", "Rocky");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-016");
        createVisit(receptionistToken, petId, vetId, "2026-11-03T10:00:00", "First appointment");

        String nonConflictingBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-11-03T10:16:00", "Later appointment"));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonConflictingBody))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelledVisitIsExcludedFromOverlapCheck() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-cancelled-overlap@example.com", "Max");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-017");
        long firstVisitId = createVisit(receptionistToken, petId, vetId, "2026-11-04T10:00:00", "First appointment");

        String cancelBody = objectMapper.writeValueAsString(new VisitStatusPayload("CANCELLED"));
        mockMvc.perform(patch("/api/visits/" + firstVisitId + "/status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk());

        String sameSlotBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-11-04T10:05:00", "Replacement appointment"));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameSlotBody))
                .andExpect(status().isCreated());
    }

    @Test
    void updatingVisitToOverlapWithAnotherVisitReturnsConflict() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-update-overlap@example.com", "Nala");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-018");
        createVisit(receptionistToken, petId, vetId, "2026-11-05T10:00:00", "First appointment");
        long secondVisitId = createVisit(receptionistToken, petId, vetId, "2026-11-05T12:00:00", "Second appointment");

        String updateBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-11-05T10:05:00", "Rescheduled into conflict"));

        mockMvc.perform(put("/api/visits/" + secondVisitId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isConflict());
    }

    @Test
    void updatingVisitWithoutChangingTimeDoesNotConflictWithItself() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petId = createPet(receptionistToken, "visit-self-update@example.com", "Buddy");
        long vetId = createVet(receptionistToken, "VET-LIC-VISIT-019");
        long visitId = createVisit(receptionistToken, petId, vetId, "2026-11-06T10:00:00", "Original complaint");

        String updateBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-11-06T10:00:00", "Updated complaint, same time"));

        mockMvc.perform(put("/api/visits/" + visitId)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chiefComplaint").value("Updated complaint, same time"));
    }

    private long createVisit(String token, long petId, long vetId, String scheduledAt, String chiefComplaint) throws Exception {
        String createBody = objectMapper.writeValueAsString(new VisitPayload(petId, vetId, scheduledAt, chiefComplaint));

        String response = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createPetWithAllergies(String token, String ownerEmail, String petName, String allergies) throws Exception {
        long ownerId = createOwner(token, ownerEmail);

        String petBody = objectMapper.writeValueAsString(
                new PetPayload(ownerId, petName, "DOG", "Golden Retriever", null,
                        "2022-03-15", "FEMALE", 24.5, allergies, null));

        String response = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody))
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
                new VetPayload("Dr. Visit Test", "Surgery", licenseNo, "Mon-Fri 09:00-17:00"));

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

    private record VisitStatusPayload(String status) {
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
