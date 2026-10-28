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
@Table(name = "floors")
public class Floor {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "property_id", updatable = false)
    private UUID propertyId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "building_id", updatable = false)
    private UUID buildingId;
    @Column(name = "level_number")
    private int levelNumber;
    private String label;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Floor() {
    }

    public Floor(UUID propertyId, UUID buildingId, int levelNumber, String label, Instant now) {
        this.id = UUID.randomUUID();
        this.propertyId = propertyId;
        this.buildingId = buildingId;
        this.levelNumber = levelNumber;
        this.label = label;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPropertyId() { return propertyId; }
    public UUID getBuildingId() { return buildingId; }
    public int getLevelNumber() { return levelNumber; }
    public String getLabel() { return label; }
}
