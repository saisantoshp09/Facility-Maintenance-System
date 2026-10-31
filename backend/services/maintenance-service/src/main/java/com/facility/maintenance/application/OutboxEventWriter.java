package com.facility.maintenance.application;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.facility.maintenance.domain.OutboxEvent;
import com.facility.maintenance.domain.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OutboxEventWriter {
    private final OutboxEventRepository events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxEventWriter(OutboxEventRepository events, ObjectMapper objectMapper, Clock clock) {
        this.events = events;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(String type, UUID actorId, UUID requesterId, UUID assigneeId, UUID workOrderId,
            String subject, String message) {
        Set<UUID> recipients = new LinkedHashSet<>();
        if (requesterId != null && !requesterId.equals(actorId)) recipients.add(requesterId);
        if (assigneeId != null && !assigneeId.equals(actorId)) recipients.add(assigneeId);
        if (recipients.isEmpty()) return;
        UUID eventId = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent(eventId, type, clock.instant(),
                java.util.List.copyOf(recipients), truncate(subject, 200), truncate(message, 2000),
                "WORK_ORDER", workOrderId);
        try {
            events.save(new OutboxEvent(eventId, type, objectMapper.writeValueAsString(event), clock.instant()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize notification event", e);
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
