package com.efe.veterinaryclinic.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

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
    void adminRegistersNewUserThenNewUserLogsInAndFetchesMe() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        String registerBody = objectMapper.writeValueAsString(
                new RegisterPayload("Ayse Yilmaz", "new.receptionist@clinic.com", "Secret123!", "RECEPTIONIST"));

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.receptionist@clinic.com"))
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"))
                .andExpect(jsonPath("$.token").doesNotExist());

        String newUserToken = loginAndGetToken("new.receptionist@clinic.com", "Secret123!");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.receptionist@clinic.com"))
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"));
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String registerBody = objectMapper.writeValueAsString(
                new RegisterPayload("Mehmet Demir", "new.vet@clinic.com", "Secret123!", "VET"));

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registerByNonAdminIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String registerBody = objectMapper.writeValueAsString(
                new RegisterPayload("Someone Else", "someone.else@clinic.com", "Secret123!", "VET"));

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerWithWeakPasswordReturnsValidationError() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String registerBody = objectMapper.writeValueAsString(
                new RegisterPayload("Weak Password", "weak.password@clinic.com", "alllowercase123", "VET"));

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                new LoginPayload(SEED_ADMIN_EMAIL, "wrong-password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminResetsPasswordThenUserLogsInWithNewPassword() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);

        String registerBody = objectMapper.writeValueAsString(
                new RegisterPayload("Reset Target", "reset.target@clinic.com", "OldPassword1!", "VET"));

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(registerResponse).get("id").asLong();

        String resetBody = objectMapper.writeValueAsString(new ResetPasswordPayload("NewPassword1!"));

        mockMvc.perform(patch("/api/auth/users/{id}/reset-password", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("reset.target@clinic.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // old password no longer works
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("reset.target@clinic.com", "OldPassword1!"))))
                .andExpect(status().isUnauthorized());

        // new password works
        loginAndGetToken("reset.target@clinic.com", "NewPassword1!");
    }

    @Test
    void resetPasswordByNonAdminIsForbidden() throws Exception {
        String receptionistToken = loginAndGetToken(SEED_RECEPTIONIST_EMAIL, SEED_RECEPTIONIST_PASSWORD);
        String resetBody = objectMapper.writeValueAsString(new ResetPasswordPayload("NewPassword1!"));

        mockMvc.perform(patch("/api/auth/users/{id}/reset-password", 1)
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void resetPasswordForNonexistentUserReturnsNotFound() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String resetBody = objectMapper.writeValueAsString(new ResetPasswordPayload("NewPassword1!"));

        mockMvc.perform(patch("/api/auth/users/{id}/reset-password", 999999)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void resetPasswordTooShortReturnsValidationError() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String resetBody = objectMapper.writeValueAsString(new ResetPasswordPayload("Sh0rt!"));

        mockMvc.perform(patch("/api/auth/users/{id}/reset-password", 1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordWithoutRequiredCharacterClassesReturnsValidationError() throws Exception {
        String adminToken = loginAndGetToken(SEED_ADMIN_EMAIL, SEED_ADMIN_PASSWORD);
        String resetBody = objectMapper.writeValueAsString(new ResetPasswordPayload("alllowercase123"));

        mockMvc.perform(patch("/api/auth/users/{id}/reset-password", 1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isBadRequest());
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

    private record RegisterPayload(String fullName, String email, String password, String role) {
    }

    private record LoginPayload(String email, String password) {
    }

    private record ResetPasswordPayload(String newPassword) {
    }
}
