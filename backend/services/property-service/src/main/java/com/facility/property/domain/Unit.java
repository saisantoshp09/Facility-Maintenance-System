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
@Table(name = "units")
public class Unit {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "property_id", updatable = false)
    private UUID propertyId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "building_id")
    private UUID buildingId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "floor_id")
    private UUID floorId;
    @Column(name = "unit_number")
    private String unitNumber;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Unit() {
    }

    public Unit(UUID propertyId, UUID buildingId, UUID floorId, String unitNumber, Instant now) {
        this.id = UUID.randomUUID();
        this.propertyId = propertyId;
        this.buildingId = buildingId;
        this.floorId = floorId;
        this.unitNumber = unitNumber;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPropertyId() { return propertyId; }
    public UUID getBuildingId() { return buildingId; }
    public UUID getFloorId() { return floorId; }
    public String getUnitNumber() { return unitNumber; }
}
