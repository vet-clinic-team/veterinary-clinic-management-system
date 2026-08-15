package com.efe.veterinaryclinic.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceControllerTest {

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
    void receptionistCreatesInvoiceThenCalculatesSubtotalVatAndTotal() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long visitId = createVisit(receptionistToken, "invoice-owner@example.com", "Boncuk", "VET-LIC-INV-001");

        String createBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")),
                new InvoiceItemPayload("Rabies vaccine", "VACCINATION", 1, new BigDecimal("300.00")))));

        String response = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitId").value(visitId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(800.00))
                .andExpect(jsonPath("$.vatRate").value(0.18))
                .andExpect(jsonPath("$.vatAmount").value(144.00))
                .andExpect(jsonPath("$.total").value(944.00))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(500.00))
                .andExpect(jsonPath("$.items[1].lineTotal").value(300.00))
                .andReturn().getResponse().getContentAsString();

        long invoiceId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/invoices/" + invoiceId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId))
                .andExpect(jsonPath("$.total").value(944.00));
    }

    @Test
    void createInvoiceRoundsVatAmountToTwoDecimalPlaces() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long visitId = createVisit(receptionistToken, "invoice-rounding@example.com", "Minnos", "VET-LIC-INV-002");

        String createBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Odd-priced item", "OTHER", 1, new BigDecimal("33.33")))));

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(33.33))
                .andExpect(jsonPath("$.vatAmount").value(6.00))
                .andExpect(jsonPath("$.total").value(39.33));
    }

    @Test
    void createInvoiceIgnoresClientSubmittedTotals() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long visitId = createVisit(receptionistToken, "invoice-spoofed-totals@example.com", "Sahte", "VET-LIC-INV-015");

        String createBody = """
                {
                  "visitId": %d,
                  "subtotal": 1.00,
                  "vatAmount": 1.00,
                  "total": 1.00,
                  "items": [
                    {"description": "Consultation", "category": "CONSULTATION", "quantity": 1, "unitPrice": 500.00}
                  ]
                }
                """.formatted(visitId);

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(500.00))
                .andExpect(jsonPath("$.vatAmount").value(90.00))
                .andExpect(jsonPath("$.total").value(590.00));
    }

    @Test
    void createInvoiceByVetIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long visitId = createVisit(receptionistToken, "invoice-forbidden@example.com", "Tekir", "VET-LIC-INV-003");

        String createBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInvoiceWithEmptyItemsReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long visitId = createVisit(receptionistToken, "invoice-empty-items@example.com", "Karabas", "VET-LIC-INV-004");

        String createBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of()));

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items"));
    }

    @Test
    void createInvoiceWithUnknownVisitReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        String createBody = objectMapper.writeValueAsString(new InvoicePayload(999999L, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void listInvoicesFilteredByStatusReturnsOnlyMatchingInvoices() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long visitId = createVisit(receptionistToken, "invoice-list-status@example.com", "Duman", "VET-LIC-INV-005");
        String createBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.visitId == " + visitId + ")]").exists());

        mockMvc.perform(get("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.visitId == " + visitId + ")]").doesNotExist());
    }

    @Test
    void listInvoicesFilteredByDateRangeExcludesInvoicesOutsideRange() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long visitId = createVisit(receptionistToken, "invoice-list-range@example.com", "Coco", "VET-LIC-INV-006");
        String createBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.visitId == " + visitId + ")]").exists());

        mockMvc.perform(get("/api/invoices")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .param("from", "2000-01-01")
                        .param("to", "2000-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.visitId == " + visitId + ")]").doesNotExist());
    }

    @Test
    void petInvoiceHistoryReturnsOnlyThatPetsInvoices() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long petIdA = createPet(receptionistToken, "invoice-pet-history-a@example.com", "Duman");
        long petIdB = createPet(receptionistToken, "invoice-pet-history-b@example.com", "Kayra");
        long vetIdA = createVet(receptionistToken, "VET-LIC-INV-PET-001");
        long vetIdB = createVet(receptionistToken, "VET-LIC-INV-PET-002");
        long visitIdA = createVisitForPetAndVet(receptionistToken, petIdA, vetIdA);
        createVisitForPetAndVet(receptionistToken, petIdB, vetIdB);
        long invoiceIdA = createInvoiceForVisit(receptionistToken, visitIdA);

        mockMvc.perform(get("/api/pets/" + petIdA + "/invoices")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(invoiceIdA))
                .andExpect(jsonPath("$.content[0].visitId").value(visitIdA));
    }

    @Test
    void petInvoiceHistoryForUnknownPetReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/pets/999999/invoices")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUnknownInvoiceReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(get("/api/invoices/999999").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionistMarksInvoiceAsSent() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long invoiceId = createInvoice(receptionistToken, "invoice-send@example.com", "Zorro", "VET-LIC-INV-007");

        mockMvc.perform(patch("/api/invoices/" + invoiceId + "/send")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void sendInvoiceByVetIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long invoiceId = createInvoice(receptionistToken, "invoice-send-forbidden@example.com", "Findik", "VET-LIC-INV-013");

        mockMvc.perform(patch("/api/invoices/" + invoiceId + "/send")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendUnknownInvoiceReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(patch("/api/invoices/999999/send")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionistMarksInvoiceAsPaid() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long invoiceId = createInvoice(receptionistToken, "invoice-paid@example.com", "Şans", "VET-LIC-INV-008");

        mockMvc.perform(patch("/api/invoices/" + invoiceId + "/mark-paid")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void markPaidByVetIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long invoiceId = createInvoice(receptionistToken, "invoice-paid-forbidden@example.com", "Maya", "VET-LIC-INV-009");

        mockMvc.perform(patch("/api/invoices/" + invoiceId + "/mark-paid")
                        .header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void markPaidForUnknownInvoiceReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        mockMvc.perform(patch("/api/invoices/999999/mark-paid")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkMarkPaidUpdatesAllRequestedInvoices() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long invoiceIdA = createInvoice(receptionistToken, "invoice-bulk-a@example.com", "Pati", "VET-LIC-INV-010");
        long invoiceIdB = createInvoice(receptionistToken, "invoice-bulk-b@example.com", "Toprak", "VET-LIC-INV-011");

        String bulkBody = objectMapper.writeValueAsString(new BulkMarkPaidPayload(List.of(invoiceIdA, invoiceIdB)));

        mockMvc.perform(patch("/api/invoices/bulk-mark-paid")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == " + invoiceIdA + ")].status").value("PAID"))
                .andExpect(jsonPath("$[?(@.id == " + invoiceIdB + ")].status").value("PAID"));
    }

    @Test
    void bulkMarkPaidByVetIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        long invoiceId = createInvoice(receptionistToken, "invoice-bulk-forbidden@example.com", "Minnak", "VET-LIC-INV-014");

        String bulkBody = objectMapper.writeValueAsString(new BulkMarkPaidPayload(List.of(invoiceId)));

        mockMvc.perform(patch("/api/invoices/bulk-mark-paid")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkMarkPaidWithEmptyListReturnsValidationError() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String bulkBody = objectMapper.writeValueAsString(new BulkMarkPaidPayload(List.of()));

        mockMvc.perform(patch("/api/invoices/bulk-mark-paid")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("invoiceIds"));
    }

    @Test
    void bulkMarkPaidWithUnknownInvoiceIdReturnsNotFound() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long invoiceId = createInvoice(receptionistToken, "invoice-bulk-unknown@example.com", "Çomar", "VET-LIC-INV-012");

        String bulkBody = objectMapper.writeValueAsString(new BulkMarkPaidPayload(List.of(invoiceId, 999999L)));

        mockMvc.perform(patch("/api/invoices/bulk-mark-paid")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void invoiceStatsReflectTotalsAndStatusCountsAfterCreatingAndPayingAnInvoice() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        long totalBefore = fetchInvoiceStatsField(receptionistToken, "totalInvoices");
        long draftBefore = fetchInvoiceStatsField(receptionistToken, "draft");
        long paidBefore = fetchInvoiceStatsField(receptionistToken, "paid");

        long invoiceId = createInvoice(receptionistToken, "invoice-stats@example.com", "Stats Pati", "VET-LIC-INV-STATS");

        mockMvc.perform(get("/api/invoices/stats").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoices").value(totalBefore + 1))
                .andExpect(jsonPath("$.draft").value(draftBefore + 1))
                .andExpect(jsonPath("$.paid").value(paidBefore));

        mockMvc.perform(patch("/api/invoices/" + invoiceId + "/mark-paid")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/invoices/stats").header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft").value(draftBefore))
                .andExpect(jsonPath("$.paid").value(paidBefore + 1));
    }

    private long fetchInvoiceStatsField(String token, String field) throws Exception {
        String response = mockMvc.perform(get("/api/invoices/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get(field).asLong();
    }

    private long createInvoice(String token, String ownerEmail, String petName, String licenseNo) throws Exception {
        long visitId = createVisit(token, ownerEmail, petName, licenseNo);

        return createInvoiceForVisit(token, visitId);
    }

    private long createInvoiceForVisit(String token, long visitId) throws Exception {
        String createBody = objectMapper.writeValueAsString(new InvoicePayload(visitId, List.of(
                new InvoiceItemPayload("Consultation", "CONSULTATION", 1, new BigDecimal("500.00")))));

        String response = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createVisit(String token, String ownerEmail, String petName, String licenseNo) throws Exception {
        long petId = createPet(token, ownerEmail, petName);
        long vetId = createVet(token, licenseNo);

        return createVisitForPetAndVet(token, petId, vetId);
    }

    private long createVisitForPetAndVet(String token, long petId, long vetId) throws Exception {
        String createBody = objectMapper.writeValueAsString(
                new VisitPayload(petId, vetId, "2026-07-10T14:30:00", "Annual checkup"));

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
                new VetPayload("Dr. Invoice Test", "Surgery", licenseNo, "Mon-Fri 09:00-17:00"));

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

    private record InvoicePayload(Long visitId, List<InvoiceItemPayload> items) {
    }

    private record InvoiceItemPayload(String description, String category, Integer quantity, BigDecimal unitPrice) {
    }

    private record BulkMarkPaidPayload(List<Long> invoiceIds) {
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
