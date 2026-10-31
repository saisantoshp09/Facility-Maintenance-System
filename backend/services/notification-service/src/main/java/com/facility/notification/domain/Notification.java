package com.facility.notification.domain;

import java.time.Duration;
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
import jakarta.persistence.Version;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "event_id", updatable = false)
    private UUID eventId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "recipient_user_id", updatable = false)
    private UUID recipientUserId;
    @Column(updatable = false)
    private String type;
    @Column(updatable = false)
    private String subject;
    @Column(updatable = false)
    private String message;
    @Column(name = "reference_type", updatable = false)
    private String referenceType;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private int attempts;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Version
    private Long version;

    protected Notification() {
    }

    public Notification(UUID eventId, UUID recipientUserId, String type, String subject, String message,
            String referenceType, UUID referenceId, Instant now) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.subject = subject;
        this.message = message;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = NotificationStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public void markSent() {
        attempts++;
        status = NotificationStatus.SENT;
        nextAttemptAt = null;
    }

    /** Records a failed attempt; schedules a retry with the given delay or gives up at maxAttempts. */
    public void markFailedAttempt(int maxAttempts, Duration retryDelay, Instant now) {
        attempts++;
        if (attempts >= maxAttempts) {
            status = NotificationStatus.FAILED;
            nextAttemptAt = null;
        } else {
            nextAttemptAt = now.plus(retryDelay);
        }
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public String getType() { return type; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
