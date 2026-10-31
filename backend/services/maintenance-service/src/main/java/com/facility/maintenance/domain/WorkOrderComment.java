package com.facility.maintenance.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "work_order_comments")
public class WorkOrderComment {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "work_order_id", updatable = false)
    private UUID workOrderId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "author_id", updatable = false)
    private UUID authorId;
    private String body;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected WorkOrderComment() {
    }

    public WorkOrderComment(UUID workOrderId, UUID authorId, String body, Instant now) {
        this.id = UUID.randomUUID();
        this.workOrderId = workOrderId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getWorkOrderId() { return workOrderId; }
    public UUID getAuthorId() { return authorId; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
