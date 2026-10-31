package com.facility.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.facility.identity.application.UserAdministrationService;
import com.facility.identity.domain.AuditEventRepository;
import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccount;
import com.facility.identity.domain.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityApiIntegrationTest {
    private static final String PASSWORD = "synthetic-test-passphrase-1";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAdministrationService administration;
    @Autowired UserAccountRepository users;
    @Autowired AuditEventRepository audits;

    UserAccount admin;
    UserAccount tenant;

    @BeforeEach
    void setUp() {
        audits.deleteAll();
        users.deleteAll();
        admin = administration.createUser(null, "Admin@Example.test", "Admin", PASSWORD, Set.of(Role.SYSTEM_ADMIN));
        tenant = administration.createUser(null, "tenant@example.test", "Tenant", PASSWORD, Set.of(Role.TENANT));
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("access_token").asText();
    }

    @Test
    void loginIssuesBearerTokenAndNormalizesEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  ADMIN@example.test \",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900));
    }

    @Test
    void wrongPasswordAndUnknownUserBothReturnGeneric401() throws Exception {
        for (String email : new String[] {"admin@example.test", "nobody@example.test"}) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password-value\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
        }
    }

    @Test
    void disabledAccountCannotLoginOrUseExistingToken() throws Exception {
        String tenantToken = login("tenant@example.test", PASSWORD);
        String adminToken = login("admin@example.test", PASSWORD);
        mvc.perform(patch("/api/v1/users/" + tenant.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tenant@example.test\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUserWithoutSecrets() throws Exception {
        String token = login("tenant@example.test", PASSWORD);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tenant@example.test"))
                .andExpect(jsonPath("$.roles[0]").value("TENANT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void missingOrInvalidTokenIsRejectedWithErrorEnvelope() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("X-Correlation-ID", "corr-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "corr-123"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.correlationId").value("corr-123"));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdministratorsCannotManageUsers() throws Exception {
        String token = login("tenant@example.test", PASSWORD);
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.test\",\"displayName\":\"X\",\"password\":\"" + PASSWORD
                                + "\",\"roles\":[\"SYSTEM_ADMIN\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCreatesAndListsUsers() throws Exception {
        String token = login("admin@example.test", PASSWORD);
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tech@example.test\",\"displayName\":\"Tech\",\"password\":\""
                                + PASSWORD + "\",\"roles\":[\"TECHNICIAN\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/users/")))
                .andExpect(jsonPath("$.roles[0]").value("TECHNICIAN"));
        mvc.perform(get("/api/v1/users?size=2&sort=email,asc").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].email").value("admin@example.test"));
        mvc.perform(get("/api/v1/users?sort=passwordHash").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailReturns409AndInvalidInputReturns400() throws Exception {
        String token = login("admin@example.test", PASSWORD);
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"TENANT@example.test\",\"displayName\":\"Dup\",\"password\":\""
                                + PASSWORD + "\",\"roles\":[\"TENANT\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"displayName\":\"\",\"password\":\"short\",\"roles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void administratorCannotDisableOwnAccount() throws Exception {
        String token = login("admin@example.test", PASSWORD);
        mvc.perform(patch("/api/v1/users/" + admin.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void jwksExposesOnlyPublicKeyMaterial() throws Exception {
        mvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").exists())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist());
    }

    @Test
    void passwordIsStoredHashedNotPlain() {
        String hash = users.findByEmail("tenant@example.test").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2").doesNotContain(PASSWORD);
    }
}
