package com.efe.veterinaryclinic.support;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupportRequestControllerTest {

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
    void vetCreatesRequestThenCanFetchItById() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        long requestId = createSupportRequest(vetToken, "Login issue", "Cannot log in after password reset");

        mockMvc.perform(get("/api/support-requests/" + requestId).header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.subject").value("Login issue"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void receptionistCannotViewAnotherUsersRequestById() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        long requestId = createSupportRequest(vetToken, "Vet-only issue", "Something only the vet reported");

        mockMvc.perform(get("/api/support-requests/" + requestId).header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSeesAllRequestsWhileNonAdminSeesOnlyOwn() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        createSupportRequest(vetToken, "Vet ticket A", "message A");
        createSupportRequest(receptionistToken, "Receptionist ticket B", "message B");

        mockMvc.perform(get("/api/support-requests").header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.subject == 'Receptionist ticket B')]").isEmpty());

        mockMvc.perform(get("/api/support-requests").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.subject == 'Vet ticket A')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.subject == 'Receptionist ticket B')]").isNotEmpty());
    }

    @Test
    void adminUpdatesStatusAndSetsAdminResponse() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        long requestId = createSupportRequest(vetToken, "Needs resolving", "Please help");

        String statusBody = objectMapper.writeValueAsString(new StatusUpdatePayload("RESOLVED", "Fixed, please retry"));

        mockMvc.perform(patch("/api/support-requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminResponse").value("Fixed, please retry"));
    }

    @Test
    void nonAdminUpdatingStatusIsForbidden() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        long requestId = createSupportRequest(vetToken, "Ticket to hijack", "message");

        String statusBody = objectMapper.writeValueAsString(new StatusUpdatePayload("RESOLVED", "not allowed"));

        mockMvc.perform(patch("/api/support-requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingRequestSucceedsWithNotificationsDisabled() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);

        String createBody = objectMapper.writeValueAsString(
                new CreateRequestPayload("Printer broken", "The invoice printer is not working"));

        mockMvc.perform(post("/api/support-requests")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createSupportRequestWithSubjectAtMaxLengthSucceeds() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String subject = "S".repeat(255);

        String createBody = objectMapper.writeValueAsString(new CreateRequestPayload(subject, "message"));

        mockMvc.perform(post("/api/support-requests")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value(subject));
    }

    @Test
    void createSupportRequestWithSubjectOverMaxLengthReturnsValidationError() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        String createBody = objectMapper.writeValueAsString(new CreateRequestPayload("S".repeat(256), "message"));

        mockMvc.perform(post("/api/support-requests")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("subject"));
    }

    @Test
    void createSupportRequestWithMessageAtMaxLengthSucceeds() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String message = "M".repeat(4000);

        String createBody = objectMapper.writeValueAsString(new CreateRequestPayload("subject", message));

        mockMvc.perform(post("/api/support-requests")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void createSupportRequestWithMessageOverMaxLengthReturnsValidationError() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        String createBody = objectMapper.writeValueAsString(new CreateRequestPayload("subject", "M".repeat(4001)));

        mockMvc.perform(post("/api/support-requests")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("message"));
    }

    @Test
    void createSupportRequestWithOversizedFieldDoesNotLeakDatabaseDetails() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);

        String createBody = objectMapper.writeValueAsString(new CreateRequestPayload("subject", "M".repeat(5000)));

        String response = mockMvc.perform(post("/api/support-requests")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(response.toLowerCase())
                .doesNotContain("sql", "column", "varchar", "psqlexception");
    }

    @Test
    void updateStatusWithAdminResponseAtMaxLengthSucceeds() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long requestId = createSupportRequest(vetToken, "Needs resolving", "Please help");
        String adminResponse = "R".repeat(4000);

        String statusBody = objectMapper.writeValueAsString(new StatusUpdatePayload("RESOLVED", adminResponse));

        mockMvc.perform(patch("/api/support-requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminResponse").value(adminResponse));
    }

    @Test
    void updateStatusWithAdminResponseOverMaxLengthReturnsValidationError() throws Exception {
        String vetToken = loginAndGetToken(SEED_VET1_EMAIL, SEED_VET1_PASSWORD);
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        long requestId = createSupportRequest(vetToken, "Needs resolving", "Please help");

        String statusBody = objectMapper.writeValueAsString(new StatusUpdatePayload("RESOLVED", "R".repeat(4001)));

        mockMvc.perform(patch("/api/support-requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("adminResponse"));
    }

    private long createSupportRequest(String token, String subject, String message) throws Exception {
        String createBody = objectMapper.writeValueAsString(new CreateRequestPayload(subject, message));

        String response = mockMvc.perform(post("/api/support-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
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

    private record CreateRequestPayload(String subject, String message) {
    }

    private record StatusUpdatePayload(String status, String adminResponse) {
    }

    private record LoginPayload(String email, String password) {
    }
}
