package com.facility.notification.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.facility.notification.application.DeliveryService;
import com.facility.notification.domain.DeliveryAttempt;
import com.facility.notification.domain.DeliveryAttemptRepository;
import com.facility.notification.domain.DeliveryOutcome;
import com.facility.notification.domain.Notification;
import com.facility.notification.domain.NotificationRepository;
import com.facility.notification.domain.NotificationStatus;
import com.facility.notification.domain.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(NotificationApiIntegrationTest.ClockConfig.class)
class NotificationApiIntegrationTest {
    static class MutableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    static final String KEY = "test-internal-key";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;
    @Autowired DeliveryService delivery;
    @Autowired NotificationRepository notifications;
    @Autowired DeliveryAttemptRepository attempts;
    @Autowired ProcessedEventRepository processed;

    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();

    @BeforeEach
    void clean() {
        attempts.deleteAll();
        notifications.deleteAll();
        processed.deleteAll();
        clock.now.set(Instant.parse("2026-01-01T00:00:00Z"));
    }

    String event(UUID eventId, String type, List<UUID> recipients) {
        String ids = String.join(",", recipients.stream().map(u -> "\"" + u + "\"").toList());
        return "{\"eventId\":\"" + eventId + "\",\"type\":\"" + type + "\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"recipientUserIds\":[" + ids + "],\"subject\":\"Work order updated\","
                + "\"message\":\"Status changed\",\"referenceType\":\"WORK_ORDER\",\"referenceId\":\""
                + UUID.randomUUID() + "\"}";
    }

    void ingest(String body, int expected) throws Exception {
        mvc.perform(post("/internal/v1/events").header("X-Internal-Api-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(expected));
    }

    RequestPostProcessor as(UUID user) {
        return jwt().jwt(token -> token.subject(user.toString()));
    }

    JsonNode listFor(UUID user, String query) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/notifications" + query).with(as(user)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    UUID onlyNotificationOf(UUID user) throws Exception {
        return UUID.fromString(listFor(user, "").get("content").get(0).get("id").asText());
    }

    @Test
    void ingestionRejectsMissingOrWrongKey() throws Exception {
        String body = event(UUID.randomUUID(), "T", List.of(alice));
        mvc.perform(post("/internal/v1/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/events").header("X-Internal-Api-Key", "wrong")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/events").with(as(alice)).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isUnauthorized());
        assertThat(notifications.count()).isZero();
    }

    @Test
    void duplicateEventCreatesNothing() throws Exception {
        UUID eventId = UUID.randomUUID();
        String body = event(eventId, "T", List.of(alice));
        mvc.perform(post("/internal/v1/events").header("X-Internal-Api-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.notifications", is(1)));
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/internal/v1/events").header("X-Internal-Api-Key", KEY)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("DUPLICATE")));
        }
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(processed.count()).isEqualTo(1);
    }

    @Test
    void fansOutToEachRecipient() throws Exception {
        mvc.perform(post("/internal/v1/events").header("X-Internal-Api-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(event(UUID.randomUUID(), "T", List.of(alice, bob, alice))))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.notifications", is(2)));
        listFor(alice, "");
        mvc.perform(get("/api/v1/notifications").with(as(bob))).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));
    }

    @Test
    void validationErrorsReturn400() throws Exception {
        ingest("{}", 400);
        ingest(event(UUID.randomUUID(), "T", List.of()), 400);
        ingest(event(UUID.randomUUID(), "x".repeat(65), List.of(alice)), 400);
        ingest(event(UUID.randomUUID(), "T", java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> UUID.randomUUID()).toList()), 400);
        ingest("not json", 400);
        assertThat(processed.count()).isZero();
    }

    @Test
    void deliverySuccessRecordsAttempt() throws Exception {
        ingest(event(UUID.randomUUID(), "T", List.of(alice)), 202);
        assertThat(delivery.deliverDue()).isEqualTo(1);
        assertThat(delivery.deliverDue()).isZero();
        Notification n = notifications.findAll().get(0);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getAttempts()).isEqualTo(1);
        List<DeliveryAttempt> list = attempts.findByNotificationIdOrderByAttemptNoAsc(n.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getOutcome()).isEqualTo(DeliveryOutcome.SUCCESS);
    }

    @Test
    void failureRetriesWithBackoffThenFails() throws Exception {
        ingest(event(UUID.randomUUID(), "ALWAYS_FAIL", List.of(alice)), 202);
        UUID id = notifications.findAll().get(0).getId();

        assertThat(delivery.deliverDue()).isEqualTo(1);
        Notification n = notifications.findById(id).orElseThrow();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getNextAttemptAt()).isEqualTo(clock.now.get().plus(Duration.ofSeconds(30)));
        assertThat(delivery.deliverDue()).isZero();

        clock.now.set(clock.now.get().plusSeconds(31));
        assertThat(delivery.deliverDue()).isEqualTo(1);
        n = notifications.findById(id).orElseThrow();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getNextAttemptAt()).isEqualTo(clock.now.get().plus(Duration.ofSeconds(60)));
        assertThat(delivery.deliverDue()).isZero();

        clock.now.set(clock.now.get().plusSeconds(61));
        assertThat(delivery.deliverDue()).isEqualTo(1);
        n = notifications.findById(id).orElseThrow();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.getAttempts()).isEqualTo(3);
        clock.now.set(clock.now.get().plusSeconds(1000));
        assertThat(delivery.deliverDue()).isZero();

        UUID admin = UUID.randomUUID();
        mvc.perform(get("/api/v1/admin/notifications/" + id + "/attempts")
                .with(jwt().jwt(t -> t.subject(admin.toString())).authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].attemptNo", is(1))).andExpect(jsonPath("$[2].outcome", is("FAILURE")))
                .andExpect(jsonPath("$[2].error", is("Simulated delivery failure")));
    }

    @Test
    void adminAttemptsRestrictedToSystemAdmin() throws Exception {
        ingest(event(UUID.randomUUID(), "T", List.of(alice)), 202);
        UUID id = onlyNotificationOf(alice);
        mvc.perform(get("/api/v1/admin/notifications/" + id + "/attempts").with(as(alice)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/notifications/" + UUID.randomUUID() + "/attempts")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersOnlySeeOwnNotifications() throws Exception {
        ingest(event(UUID.randomUUID(), "T", List.of(alice)), 202);
        UUID id = onlyNotificationOf(alice);
        mvc.perform(post("/api/v1/notifications/" + id + "/read").with(as(bob))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/notifications").with(as(bob))).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/v1/notifications/unread-count").with(as(bob))).andExpect(jsonPath("$.count", is(0)));
    }

    @Test
    void markReadIsIdempotentAndUpdatesUnreadCount() throws Exception {
        ingest(event(UUID.randomUUID(), "T", List.of(alice)), 202);
        ingest(event(UUID.randomUUID(), "T", List.of(alice)), 202);
        mvc.perform(get("/api/v1/notifications/unread-count").with(as(alice))).andExpect(jsonPath("$.count", is(2)));
        UUID id = onlyNotificationOf(alice);

        mvc.perform(post("/api/v1/notifications/" + id + "/read").with(as(alice))).andExpect(status().isOk());
        Instant firstRead = notifications.findById(id).orElseThrow().getReadAt();
        assertThat(firstRead).isNotNull();
        clock.now.set(clock.now.get().plusSeconds(60));
        mvc.perform(post("/api/v1/notifications/" + id + "/read").with(as(alice))).andExpect(status().isOk());
        assertThat(notifications.findById(id).orElseThrow().getReadAt()).isEqualTo(firstRead);

        mvc.perform(get("/api/v1/notifications/unread-count").with(as(alice))).andExpect(jsonPath("$.count", is(1)));
        mvc.perform(get("/api/v1/notifications?unread=true").with(as(alice)))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get("/api/v1/notifications?status=SENT").with(as(alice)))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/v1/notifications?status=BOGUS").with(as(alice))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notifications?sort=message").with(as(alice))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notifications?size=500").with(as(alice)))
                .andExpect(jsonPath("$.size", is(100)));
    }

    @Test
    void apiRequiresJwt() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/notifications/" + UUID.randomUUID() + "/read")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/notifications/" + UUID.randomUUID() + "/attempts"))
                .andExpect(status().isUnauthorized());
    }
}
