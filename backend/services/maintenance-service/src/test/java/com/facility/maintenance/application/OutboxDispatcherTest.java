package com.facility.maintenance.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.facility.maintenance.domain.OutboxEvent;
import com.facility.maintenance.domain.OutboxEventRepository;
import com.facility.maintenance.domain.OutboxStatus;

class OutboxDispatcherTest {
    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void marksEventsSentForSuccessfulPublisher() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "WORK_ORDER_STATUS_CHANGED", "{}", now);
        when(repository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, now, Pageable.ofSize(10))).thenReturn(List.of(event));
        OutboxDispatcher dispatcher = new OutboxDispatcher(repository, payload -> { },
                Clock.fixed(now, ZoneOffset.UTC), 10, 5, "test-key");
        dispatcher.dispatchDueEvents();
        assertEquals(OutboxStatus.SENT, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertEquals(now, event.getSentAt());
    }

    @Test
    void retriesWithExponentialBackoffThenMarksFailed() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "WORK_ORDER_STATUS_CHANGED", "{}", now);
        when(repository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, now, Pageable.ofSize(10))).thenReturn(List.of(event));
        NotificationPublisher failing = payload -> { throw new IllegalStateException("offline"); };
        OutboxDispatcher dispatcher = new OutboxDispatcher(repository, failing,
                Clock.fixed(now, ZoneOffset.UTC), 10, 2, "test-key");
        dispatcher.dispatchDueEvents();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(now.plusSeconds(1), event.getNextAttemptAt());
        assertEquals(1, event.getAttempts());

        when(repository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, now.plusSeconds(1), Pageable.ofSize(10))).thenReturn(List.of(event));
        dispatcher = new OutboxDispatcher(repository, failing,
                Clock.fixed(now.plusSeconds(1), ZoneOffset.UTC), 10, 2, "test-key");
        dispatcher.dispatchDueEvents();
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(2, event.getAttempts());
        assertEquals("offline", event.getLastError());
    }
}
