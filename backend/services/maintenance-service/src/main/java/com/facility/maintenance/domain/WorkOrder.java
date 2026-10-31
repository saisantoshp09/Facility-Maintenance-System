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
import jakarta.persistence.Version;

@Entity
@Table(name = "work_orders")
public class WorkOrder {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "property_id", updatable = false)
    private UUID propertyId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "unit_id", updatable = false)
    private UUID unitId;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private Priority priority;
    @Enumerated(EnumType.STRING)
    private WorkOrderStatus status;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "requester_id", updatable = false)
    private UUID requesterId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "assignee_id")
    private UUID assigneeId;
    @Column(name = "due_at")
    private Instant dueAt;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Version
    private long version;

    protected WorkOrder() {
    }

    public WorkOrder(UUID propertyId, UUID unitId, String title, String description, Priority priority,
            UUID requesterId, Instant dueAt, Instant now) {
        this.id = UUID.randomUUID();
        this.propertyId = propertyId;
        this.unitId = unitId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.requesterId = requesterId;
        this.dueAt = dueAt;
        this.status = WorkOrderStatus.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Applies a status change, rejecting anything outside the allowed lifecycle. */
    public void transitionTo(WorkOrderStatus target, Instant now) {
        if (!status.allowedNext().contains(target)) {
            throw new InvalidTransitionException(status, target);
        }
        this.status = target;
        this.updatedAt = now;
    }

    public void assignTo(UUID assignee, Instant dueAt, Instant now) {
        transitionTo(WorkOrderStatus.ASSIGNED, now);
        this.assigneeId = assignee;
        if (dueAt != null) {
            this.dueAt = dueAt;
        }
    }

    public void changeDueDate(Instant dueAt, Instant now) {
        if (status.isTerminalOrDone()) {
            throw new IllegalStateException("Due date cannot change after the work order is " + status);
        }
        this.dueAt = dueAt;
        this.updatedAt = now;
    }

    public boolean isOverdue(Instant now) {
        return dueAt != null && dueAt.isBefore(now) && !status.isTerminalOrDone();
    }

    public UUID getId() { return id; }
    public UUID getPropertyId() { return propertyId; }
    public UUID getUnitId() { return unitId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public UUID getRequesterId() { return requesterId; }
    public UUID getAssigneeId() { return assigneeId; }
    public Instant getDueAt() { return dueAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
