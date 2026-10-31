package com.facility.maintenance.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationEvent(UUID eventId, String type, Instant occurredAt, List<UUID> recipientUserIds,
        String subject, String message, String referenceType, UUID referenceId) {
}
