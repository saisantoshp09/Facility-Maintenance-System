package com.facility.property.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "buildings")
public class Building {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "property_id", updatable = false)
    private UUID propertyId;
    private String name;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Building() {
    }

    public Building(UUID propertyId, String name, Instant now) {
        this.id = UUID.randomUUID();
        this.propertyId = propertyId;
        this.name = name;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPropertyId() { return propertyId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
