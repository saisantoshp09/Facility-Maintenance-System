package com.facility.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "actor_id")
    private UUID actorId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(String eventType, UUID actorId, UUID subjectId, String outcome,
            String correlationId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.actorId = actorId;
        this.subjectId = subjectId;
        this.outcome = outcome;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
