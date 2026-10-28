package com.facility.maintenance.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEvent() { }

    public OutboxEvent(UUID id, String eventType, String payload, Instant createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = createdAt;
        this.createdAt = createdAt;
    }

    public void markSent(Instant now) {
        attempts++;
        status = OutboxStatus.SENT;
        sentAt = now;
        lastError = null;
    }

    public void markFailedAttempt(String error, Instant now, int maxAttempts) {
        attempts++;
        lastError = error == null ? "Notification publish failed" : error.substring(0, Math.min(error.length(), 500));
        if (attempts >= maxAttempts) {
            status = OutboxStatus.FAILED;
        } else {
            long delaySeconds = 1L << Math.min(attempts - 1, 20);
            nextAttemptAt = now.plusSeconds(delaySeconds);
        }
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
