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
@Table(name = "work_order_status_history")
public class StatusHistoryEntry {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "work_order_id", updatable = false)
    private UUID workOrderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", updatable = false)
    private WorkOrderStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", updatable = false)
    private WorkOrderStatus toStatus;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "actor_id", updatable = false)
    private UUID actorId;
    private String note;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected StatusHistoryEntry() {
    }

    public StatusHistoryEntry(UUID workOrderId, WorkOrderStatus from, WorkOrderStatus to, UUID actorId,
            String note, Instant now) {
        this.id = UUID.randomUUID();
        this.workOrderId = workOrderId;
        this.fromStatus = from;
        this.toStatus = to;
        this.actorId = actorId;
        this.note = note;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getWorkOrderId() { return workOrderId; }
    public WorkOrderStatus getFromStatus() { return fromStatus; }
    public WorkOrderStatus getToStatus() { return toStatus; }
    public UUID getActorId() { return actorId; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
