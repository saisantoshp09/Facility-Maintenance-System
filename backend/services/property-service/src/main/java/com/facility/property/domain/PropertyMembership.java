package com.facility.property.domain;

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
@Table(name = "property_memberships")
public class PropertyMembership {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "property_id", updatable = false)
    private UUID propertyId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "user_id", updatable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private MembershipRole role;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "unit_id", updatable = false)
    private UUID unitId;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected PropertyMembership() {
    }

    public PropertyMembership(UUID propertyId, UUID userId, MembershipRole role, UUID unitId, Instant now) {
        this.id = UUID.randomUUID();
        this.propertyId = propertyId;
        this.userId = userId;
        this.role = role;
        this.unitId = unitId;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPropertyId() { return propertyId; }
    public UUID getUserId() { return userId; }
    public MembershipRole getRole() { return role; }
    public UUID getUnitId() { return unitId; }
    public Instant getCreatedAt() { return createdAt; }
}
