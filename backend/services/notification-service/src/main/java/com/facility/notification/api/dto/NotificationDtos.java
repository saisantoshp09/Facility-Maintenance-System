package com.facility.notification.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.facility.notification.domain.DeliveryAttempt;
import com.facility.notification.domain.Notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record EventRequest(
            @NotNull UUID eventId,
            @NotBlank @Size(max = 64) String type,
            @NotNull Instant occurredAt,
            @NotEmpty @Size(max = 50) List<@NotNull UUID> recipientUserIds,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 2000) String message,
            @Size(max = 64) String referenceType,
            UUID referenceId) {
    }

    public record EventResponse(String status, Integer notifications) {
    }

    public record NotificationResponse(UUID id, UUID eventId, String type, String subject, String message,
            String referenceType, UUID referenceId, String status, Instant readAt, Instant createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getEventId(), n.getType(), n.getSubject(),
                    n.getMessage(), n.getReferenceType(), n.getReferenceId(), n.getStatus().name(),
                    n.getReadAt(), n.getCreatedAt());
        }
    }

    public record UnreadCountResponse(long count) {
    }

    public record AttemptResponse(UUID id, int attemptNo, String outcome, String error, Instant attemptedAt) {
        public static AttemptResponse from(DeliveryAttempt a) {
            return new AttemptResponse(a.getId(), a.getAttemptNo(), a.getOutcome().name(), a.getError(),
                    a.getAttemptedAt());
        }
    }
}
