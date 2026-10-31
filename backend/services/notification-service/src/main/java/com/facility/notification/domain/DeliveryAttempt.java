package com.facility.notification.domain;

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
@Table(name = "notification_delivery_attempts")
public class DeliveryAttempt {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "notification_id", updatable = false)
    private UUID notificationId;
    @Column(name = "attempt_no", updatable = false)
    private int attemptNo;
    @Enumerated(EnumType.STRING)
    private DeliveryOutcome outcome;
    private String error;
    @Column(name = "attempted_at", updatable = false)
    private Instant attemptedAt;

    protected DeliveryAttempt() {
    }

    public DeliveryAttempt(UUID notificationId, int attemptNo, DeliveryOutcome outcome, String error,
            Instant attemptedAt) {
        this.id = UUID.randomUUID();
        this.notificationId = notificationId;
        this.attemptNo = attemptNo;
        this.outcome = outcome;
        this.error = error == null ? null : error.substring(0, Math.min(500, error.length()));
        this.attemptedAt = attemptedAt;
    }

    public UUID getId() { return id; }
    public UUID getNotificationId() { return notificationId; }
    public int getAttemptNo() { return attemptNo; }
    public DeliveryOutcome getOutcome() { return outcome; }
    public String getError() { return error; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
