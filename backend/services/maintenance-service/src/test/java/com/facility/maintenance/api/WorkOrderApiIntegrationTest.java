package com.facility.maintenance.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.facility.maintenance.application.Caller;
import com.facility.maintenance.application.OutboxDispatcher;
import com.facility.maintenance.application.DependencyUnavailableException;
import com.facility.maintenance.application.NotFoundException;
import com.facility.maintenance.application.PropertyAccessView;
import com.facility.maintenance.application.PropertyDirectory;
import com.facility.maintenance.domain.Priority;
import com.facility.maintenance.domain.OutboxEvent;
import com.facility.maintenance.domain.OutboxEventRepository;
import com.facility.maintenance.domain.OutboxStatus;
import com.facility.maintenance.domain.StatusHistoryRepository;
import com.facility.maintenance.domain.WorkOrder;
import com.facility.maintenance.domain.WorkOrderCommentRepository;
import com.facility.maintenance.domain.WorkOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(WorkOrderApiIntegrationTest.FakeDirectoryConfig.class)
class WorkOrderApiIntegrationTest {
    /** In-memory stand-in for the property service, keyed by property and user. */
    static class FakeDirectory implements PropertyDirectory {
        final Map<String, PropertyAccessView> grants = new HashMap<>();
        volatile boolean down;

        void grant(UUID property, UUID user, String role, UUID unit) {
            grants.put(property + ":" + user, new PropertyAccessView(property, false, Set.of(role),
                    unit == null ? Set.of() : Set.of(unit)));
        }

        @Override
        public PropertyAccessView accessOf(Caller caller, UUID propertyId) {
            if (down) {
                throw new DependencyUnavailableException("down", null);
            }
            PropertyAccessView view = grants.get(propertyId + ":" + caller.userId());
            if (view == null) {
                throw new NotFoundException("Property not found");
            }
            return view;
        }

        @Override
        public PropertyAccessView memberAccess(Caller caller, UUID propertyId, UUID userId) {
            if (!accessOf(caller, propertyId).canManage()) {
                throw new AccessDeniedException("manager required");
            }
            PropertyAccessView view = grants.get(propertyId + ":" + userId);
            return view != null ? view : new PropertyAccessView(propertyId, false, Set.of(), Set.of());
        }
    }

    @TestConfiguration
    static class FakeDirectoryConfig {
        @Bean
        @Primary
        FakeDirectory fakeDirectory() {
            return new FakeDirectory();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FakeDirectory directory;
    @Autowired WorkOrderRepository orders;
    @Autowired WorkOrderCommentRepository comments;
    @Autowired StatusHistoryRepository history;
    @Autowired OutboxEventRepository outbox;
    @Autowired Clock clock;

    final UUID propA = UUID.randomUUID();
    final UUID propB = UUID.randomUUID();
    final UUID unitA1 = UUID.randomUUID();
    final UUID unitA2 = UUID.randomUUID();
    final UUID managerA = UUID.randomUUID();
    final UUID managerB = UUID.randomUUID();
    final UUID tenant1 = UUID.randomUUID();
    final UUID tenant2 = UUID.randomUUID();
    final UUID tech = UUID.randomUUID();
    final UUID otherTech = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        outbox.deleteAll();
        comments.deleteAll();
        history.deleteAll();
        orders.deleteAll();
        directory.grants.clear();
        directory.down = false;
        directory.grant(propA, managerA, "PROPERTY_MANAGER", null);
        directory.grant(propB, managerB, "PROPERTY_MANAGER", null);
        directory.grant(propA, tenant1, "TENANT", unitA1);
        directory.grant(propA, tenant2, "TENANT", unitA2);
        directory.grant(propA, tech, "TECHNICIAN", null);
        directory.grant(propA, otherTech, "TECHNICIAN", null);
    }

    RequestPostProcessor as(UUID user) {
        return jwt().jwt(token -> token.subject(user.toString()));
    }

    MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder builder0, UUID who, String body) {
        MockHttpServletRequestBuilder builder = builder0.with(as(who));
        return body == null ? builder : builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    JsonNode send(String method, String url, UUID who, String body, int expected) throws Exception {
        String response = mvc.perform(withBody(MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method), url), who, body))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? null : json.readTree(response);
    }

    String open(UUID tenant, UUID property, UUID unit, int expected) throws Exception {
        String unitJson = unit == null ? "" : ",\"unitId\":\"" + unit + "\"";
        JsonNode node = send("POST", "/api/v1/work-orders", tenant, "{\"propertyId\":\"" + property
                + "\",\"title\":\"Leak\",\"description\":\"Kitchen leak\",\"priority\":\"HIGH\"" + unitJson + "}",
                expected);
        return expected == 201 ? node.get("id").asText() : null;
    }

    JsonNode move(String id, UUID who, String target, String extra, int expected) throws Exception {
        return send("POST", "/api/v1/work-orders/" + id + "/transitions", who,
                "{\"targetStatus\":\"" + target + "\"" + extra + "}", expected);
    }


    @Test
    void notificationOutboxTracksValidTransitionsAndExcludesActor() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        org.junit.jupiter.api.Assertions.assertTrue(outbox.findAll().isEmpty());

        move(id, managerA, "APPROVED", "", 200);
        List<OutboxEvent> events = outbox.findAll();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        JsonNode payload = json.readTree(events.get(0).getPayload());
        org.junit.jupiter.api.Assertions.assertEquals("WORK_ORDER_STATUS_CHANGED", payload.get("type").asText());
        org.junit.jupiter.api.Assertions.assertEquals(List.of(tenant1.toString()),
                json.convertValue(payload.get("recipientUserIds"), json.getTypeFactory().constructCollectionType(List.class, String.class)));
        org.junit.jupiter.api.Assertions.assertFalse(payload.toString().contains(managerA.toString()));
        org.junit.jupiter.api.Assertions.assertEquals("WORK_ORDER", payload.get("referenceType").asText());

        move(id, managerA, "CLOSED", "", 409);
        org.junit.jupiter.api.Assertions.assertEquals(1, outbox.count());
        move(id, tenant1, "APPROVED", "", 403);
        org.junit.jupiter.api.Assertions.assertEquals(1, outbox.count());
    }


    @Test
    void notificationPublisherFailureDoesNotFailWorkOrderApi() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        move(id, managerA, "APPROVED", "", 200);
        OutboxDispatcher dispatcher = new OutboxDispatcher(outbox, payload -> {
            throw new IllegalStateException("notification service unavailable");
        }, clock, 10, 5, "test-key");
        dispatcher.dispatchDueEvents();
        org.junit.jupiter.api.Assertions.assertEquals(OutboxStatus.PENDING, outbox.findAll().get(0).getStatus());
        open(tenant2, propA, unitA2, 201);
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA)).andExpect(status().isUnauthorized());
    }

    @Test
    void fullLifecycleWithHistory() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        move(id, managerA, "APPROVED", "", 200);
        JsonNode assigned = move(id, managerA, "ASSIGNED", ",\"assigneeId\":\"" + tech
                + "\",\"dueAt\":\"" + Instant.now().plus(3, ChronoUnit.DAYS) + "\"", 200);
        org.junit.jupiter.api.Assertions.assertEquals(tech.toString(), assigned.get("assigneeId").asText());
        move(id, tech, "IN_PROGRESS", "", 200);
        move(id, tech, "ON_HOLD", ",\"note\":\"waiting for part\"", 200);
        move(id, tech, "IN_PROGRESS", "", 200);
        move(id, tech, "RESOLVED", "", 200);
        move(id, tenant1, "CLOSED", "", 200);

        JsonNode entries = send("GET", "/api/v1/work-orders/" + id + "/history", managerA, null, 200);
        org.junit.jupiter.api.Assertions.assertEquals(8, entries.size());
        org.junit.jupiter.api.Assertions.assertEquals("OPEN", entries.get(0).get("toStatus").asText());
        org.junit.jupiter.api.Assertions.assertEquals("CLOSED", entries.get(7).get("toStatus").asText());
    }

    @Test
    void invalidTransitionsReturn409() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        move(id, managerA, "RESOLVED", "", 409);
        move(id, managerA, "ASSIGNED", ",\"assigneeId\":\"" + tech + "\"", 409);
        move(id, managerA, "APPROVED", "", 200);
        move(id, managerA, "APPROVED", "", 409);
        move(id, managerA, "OPEN", "", 409);
    }

    @Test
    void rolesAndOwnershipAreEnforced() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        move(id, tenant1, "APPROVED", "", 403);
        move(id, tech, "APPROVED", "", 404);
        move(id, tenant2, "APPROVED", "", 404);
        move(id, managerA, "APPROVED", "", 200);
        move(id, managerA, "ASSIGNED", ",\"assigneeId\":\"" + tech + "\"", 200);
        move(id, otherTech, "IN_PROGRESS", "", 404);
        move(id, tenant1, "IN_PROGRESS", "", 403);
        move(id, tech, "IN_PROGRESS", "", 200);
        move(id, tech, "RESOLVED", "", 200);
        move(id, tenant2, "CLOSED", "", 404);
        move(id, tech, "CLOSED", "", 403);
        move(id, tenant1, "CLOSED", "", 200);
    }

    @Test
    void tenantsCanOnlyOpenForOwnUnitAndNonMembersAreHidden() throws Exception {
        open(tenant1, propA, unitA2, 403);
        open(tenant1, propA, null, 403);
        open(tech, propA, unitA1, 403);
        open(managerB, propA, null, 404);
        open(tenant1, propA, unitA1, 201);
    }

    @Test
    void assignmentAndRejectionValidation() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        move(id, managerA, "REJECTED", "", 400);
        move(id, managerA, "APPROVED", "", 200);
        move(id, managerA, "ASSIGNED", "", 400);
        move(id, managerA, "ASSIGNED", ",\"assigneeId\":\"" + tenant2 + "\"", 400);
        String second = open(tenant1, propA, unitA1, 201);
        move(second, managerA, "REJECTED", ",\"note\":\"duplicate\"", 200);
        move(second, managerA, "APPROVED", "", 409);
    }

    @Test
    void anotherPropertysDataIsNeverVisible() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        mvc.perform(get("/api/v1/work-orders/" + id).with(as(managerB))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA).with(as(managerB)))
                .andExpect(status().isNotFound());
        move(id, managerB, "APPROVED", "", 404);
        send("POST", "/api/v1/work-orders/" + id + "/comments", managerB, "{\"body\":\"hi\"}", 404);
        send("GET", "/api/v1/work-orders/" + id + "/history", managerB, null, 404);
        mvc.perform(get("/api/v1/work-orders/" + UUID.randomUUID()).with(as(managerA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsAreScopedFilteredAndPaginated() throws Exception {
        open(tenant1, propA, unitA1, 201);
        open(tenant1, propA, unitA1, 201);
        String other = open(tenant2, propA, unitA2, 201);
        move(other, managerA, "APPROVED", "", 200);

        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA).with(as(managerA)))
                .andExpect(jsonPath("$.totalElements", is(3)));
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA).with(as(tenant1)))
                .andExpect(jsonPath("$.totalElements", is(2)));
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA).with(as(tech)))
                .andExpect(jsonPath("$.totalElements", is(0)));
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA + "&status=APPROVED").with(as(managerA)))
                .andExpect(jsonPath("$.totalElements", is(1))).andExpect(jsonPath("$.content[0].id", is(other)));
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA + "&size=2&page=1").with(as(managerA)))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.totalPages", is(2)));
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA + "&sort=description").with(as(managerA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overdueFilterFindsOnlyOpenPastDueOrders() throws Exception {
        Instant now = clock.instant();
        WorkOrder late = orders.save(new WorkOrder(propA, unitA1, "Late", "d", Priority.LOW, tenant1,
                now.minus(2, ChronoUnit.DAYS), now.minus(3, ChronoUnit.DAYS)));
        orders.save(new WorkOrder(propA, unitA1, "Future", "d", Priority.LOW, tenant1,
                now.plus(2, ChronoUnit.DAYS), now));
        mvc.perform(get("/api/v1/work-orders?propertyId=" + propA + "&overdue=true").with(as(managerA)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(late.getId().toString())))
                .andExpect(jsonPath("$.content[0].overdue", is(true)));
    }

    @Test
    void commentsAreVisibleOnlyToThoseWhoCanSeeTheOrder() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        send("POST", "/api/v1/work-orders/" + id + "/comments", tenant1, "{\"body\":\"Please hurry\"}", 201);
        send("POST", "/api/v1/work-orders/" + id + "/comments", tenant2, "{\"body\":\"nope\"}", 404);
        send("POST", "/api/v1/work-orders/" + id + "/comments", managerA, "{\"body\":\"\"}", 400);
        mvc.perform(get("/api/v1/work-orders/" + id + "/comments").with(as(managerA)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void dueDateCanBeChangedOnlyByManagers() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        String body = "{\"dueAt\":\"" + Instant.now().plus(5, ChronoUnit.DAYS) + "\"}";
        send("PATCH", "/api/v1/work-orders/" + id + "/due-date", tenant1, body, 403);
        send("PATCH", "/api/v1/work-orders/" + id + "/due-date", managerA, body, 200);
        send("PATCH", "/api/v1/work-orders/" + id + "/due-date", managerA, "{\"dueAt\":\"2020-01-01T00:00:00Z\"}", 400);
    }

    @Test
    void failsClosedWhenPropertyServiceIsUnavailable() throws Exception {
        String id = open(tenant1, propA, unitA1, 201);
        directory.down = true;
        mvc.perform(get("/api/v1/work-orders/" + id).with(as(managerA))).andExpect(status().isServiceUnavailable());
        open(managerA, propA, null, 503);
    }
}
