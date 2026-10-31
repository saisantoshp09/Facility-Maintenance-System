package com.facility.property.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.facility.property.domain.PropertyMembershipRepository;
import com.facility.property.domain.PropertyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyApiIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    PropertyMembershipRepository memberships;
    @Autowired
    PropertyRepository properties;
    @Autowired
    com.facility.property.domain.UnitRepository units;
    @Autowired
    com.facility.property.domain.FloorRepository floors;
    @Autowired
    com.facility.property.domain.BuildingRepository buildings;

    final UUID adminId = UUID.randomUUID();
    final UUID managerA = UUID.randomUUID();
    final UUID tenantA = UUID.randomUUID();
    final UUID managerB = UUID.randomUUID();

    @BeforeEach
    void clean() {
        memberships.deleteAll();
        units.deleteAll();
        floors.deleteAll();
        buildings.deleteAll();
        properties.deleteAll();
    }

    RequestPostProcessor as(UUID user, String role) {
        return jwt().jwt(token -> token.subject(user.toString()).claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    JsonNode send(String url, RequestPostProcessor who, String body, int expected) throws Exception {
        String response = mvc.perform(post(url).with(who).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? null : json.readTree(response);
    }

    String createProperty(String name) throws Exception {
        return send("/api/v1/properties", as(adminId, "SYSTEM_ADMIN"), """
                {"name":"%s","addressLine":"1 Test St","city":"Testville","postalCode":"00000","country":"US"}
                """.formatted(name), 201).get("id").asText();
    }

    void member(String property, UUID user, String role, String unitId) throws Exception {
        String unit = unitId == null ? "" : ",\"unitId\":\"" + unitId + "\"";
        send("/api/v1/properties/" + property + "/memberships", as(adminId, "SYSTEM_ADMIN"),
                "{\"userId\":\"" + user + "\",\"role\":\"" + role + "\"" + unit + "}", 201);
    }

    String unit(String property, String number) throws Exception {
        return send("/api/v1/properties/" + property + "/units", as(adminId, "SYSTEM_ADMIN"),
                "{\"unitNumber\":\"" + number + "\"}", 201).get("id").asText();
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/properties")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlySystemAdminCreatesProperties() throws Exception {
        send("/api/v1/properties", as(managerA, "PROPERTY_MANAGER"), """
                {"name":"X","addressLine":"1","city":"C","postalCode":"1","country":"US"}""", 403);
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        send("/api/v1/properties", as(adminId, "SYSTEM_ADMIN"), "{\"name\":\"\",\"country\":\"usa\"}", 400);
        mvc.perform(get("/api/v1/properties?sort=password,asc").with(as(adminId, "SYSTEM_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerCannotSeeOrModifyAnotherPropertyAndListIsScoped() throws Exception {
        String a = createProperty("Alpha");
        String b = createProperty("Bravo");
        member(a, managerA, "PROPERTY_MANAGER", null);
        member(b, managerB, "PROPERTY_MANAGER", null);
        String unitB = unit(b, "B-1");

        RequestPostProcessor manager = as(managerA, "PROPERTY_MANAGER");
        mvc.perform(get("/api/v1/properties").with(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.content[0].id", is(a)));
        mvc.perform(get("/api/v1/properties/" + a).with(manager)).andExpect(status().isOk());

        mvc.perform(get("/api/v1/properties/" + b).with(manager)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/properties/" + b + "/units").with(manager)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/properties/" + b + "/units/" + unitB).with(manager))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/properties/" + b + "/memberships").with(manager))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/properties/" + b + "/access").with(manager)).andExpect(status().isNotFound());
        send("/api/v1/properties/" + b + "/buildings", manager, "{\"name\":\"Hack\"}", 404);
        send("/api/v1/properties/" + b + "/memberships", manager,
                "{\"userId\":\"" + managerA + "\",\"role\":\"PROPERTY_MANAGER\"}", 404);
    }

    @Test
    void memberAccessIsManagerOnlyAndScopedToProperty() throws Exception {
        String a = createProperty("Alpha");
        String b = createProperty("Bravo");
        member(a, managerA, "PROPERTY_MANAGER", null);
        member(a, tenantA, "TECHNICIAN", null);
        String url = "/api/v1/properties/" + a + "/members/" + tenantA + "/access";
        mvc.perform(get(url).with(as(managerA, "PROPERTY_MANAGER"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]", is("TECHNICIAN")));
        mvc.perform(get(url).with(as(tenantA, "TECHNICIAN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/properties/" + b + "/members/" + tenantA + "/access")
                .with(as(managerA, "PROPERTY_MANAGER"))).andExpect(status().isNotFound());
    }

    @Test
    void unitFromAnotherPropertyCannotBeUsedForTenantMembership() throws Exception {
        String a = createProperty("Alpha");
        String b = createProperty("Bravo");
        String unitB = unit(b, "B-1");
        send("/api/v1/properties/" + a + "/memberships", as(adminId, "SYSTEM_ADMIN"),
                "{\"userId\":\"" + tenantA + "\",\"role\":\"TENANT\",\"unitId\":\"" + unitB + "\"}", 400);
    }

    @Test
    void tenantSeesOnlyOwnUnitAndCannotManage() throws Exception {
        String a = createProperty("Alpha");
        String own = unit(a, "A-1");
        String other = unit(a, "A-2");
        member(a, tenantA, "TENANT", own);

        RequestPostProcessor tenant = as(tenantA, "TENANT");
        mvc.perform(get("/api/v1/properties/" + a + "/units").with(tenant)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.content[0].id", is(own)));
        mvc.perform(get("/api/v1/properties/" + a + "/units/" + other).with(tenant))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/properties/" + a + "/memberships").with(tenant)).andExpect(status().isForbidden());
        send("/api/v1/properties/" + a + "/buildings", tenant, "{\"name\":\"T\"}", 403);
        send("/api/v1/properties/" + a + "/units", tenant, "{\"unitNumber\":\"A-3\"}", 403);
        mvc.perform(get("/api/v1/properties/" + a + "/access").with(tenant)).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]", is("TENANT")))
                .andExpect(jsonPath("$.unitIds[0]", is(own)));
    }

    @Test
    void managerBuildsStructureAndDuplicatesConflict() throws Exception {
        String a = createProperty("Alpha");
        member(a, managerA, "PROPERTY_MANAGER", null);
        RequestPostProcessor manager = as(managerA, "PROPERTY_MANAGER");

        String building = send("/api/v1/properties/" + a + "/buildings", manager, "{\"name\":\"Tower\"}", 201)
                .get("id").asText();
        send("/api/v1/properties/" + a + "/buildings", manager, "{\"name\":\"Tower\"}", 409);
        send("/api/v1/properties/" + a + "/buildings/" + building + "/floors", manager,
                "{\"levelNumber\":1,\"label\":\"Ground\"}", 201);
        send("/api/v1/properties/" + a + "/buildings/" + building + "/floors", manager,
                "{\"levelNumber\":1}", 409);
        send("/api/v1/properties/" + a + "/units", manager,
                "{\"buildingId\":\"" + building + "\",\"unitNumber\":\"101\"}", 201);
        send("/api/v1/properties/" + a + "/units", manager, "{\"unitNumber\":\"101\"}", 409);
        mvc.perform(get("/api/v1/properties/" + a + "/buildings/" + building + "/floors").with(manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void membershipRulesAreEnforcedAndMembershipCanBeRemoved() throws Exception {
        String a = createProperty("Alpha");
        send("/api/v1/properties/" + a + "/memberships", as(adminId, "SYSTEM_ADMIN"),
                "{\"userId\":\"" + tenantA + "\",\"role\":\"TENANT\"}", 400);
        send("/api/v1/properties/" + a + "/memberships", as(adminId, "SYSTEM_ADMIN"),
                "{\"userId\":\"" + managerA + "\",\"role\":\"PROPERTY_MANAGER\"}", 201);
        send("/api/v1/properties/" + a + "/memberships", as(adminId, "SYSTEM_ADMIN"),
                "{\"userId\":\"" + managerA + "\",\"role\":\"PROPERTY_MANAGER\"}", 409);

        String id = json.readTree(mvc.perform(get("/api/v1/properties/" + a + "/memberships")
                .with(as(adminId, "SYSTEM_ADMIN"))).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString()).get("content").get(0).get("id").asText();
        mvc.perform(delete("/api/v1/properties/" + a + "/memberships/" + id).with(as(adminId, "SYSTEM_ADMIN")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/properties/" + a).with(as(managerA, "PROPERTY_MANAGER")))
                .andExpect(status().isNotFound());
    }
}
