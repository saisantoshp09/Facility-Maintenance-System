package com.facility.asset.domain;

import java.time.Instant;
import java.time.LocalDate;
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

@Entity @Table(name="assets")
public class Asset {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id = UUID.randomUUID();
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="property_id", nullable=false, columnDefinition="BINARY(16)") private UUID propertyId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="unit_id", columnDefinition="BINARY(16)") private UUID unitId;
    @Column(nullable=false, length=160) private String name;
    @Column(nullable=false, length=80) private String category;
    @Column(name="serial_number", length=120) private String serialNumber;
    @Column(nullable=false, length=200) private String location;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private AssetStatus status = AssetStatus.ACTIVE;
    @Column(name="installed_on") private LocalDate installedOn;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Version @Column(nullable=false) private long version;
    protected Asset() {}
    public Asset(UUID propertyId, UUID unitId, String name, String category, String serialNumber, String location, LocalDate installedOn, Instant now) {
        this.propertyId=propertyId; this.unitId=unitId; this.name=name; this.category=category; this.serialNumber=serialNumber; this.location=location; this.installedOn=installedOn; this.createdAt=now; this.updatedAt=now;
    }
    public void update(UUID unitId, String name, String category, String serialNumber, String location, LocalDate installedOn, Instant now) {
        if (status == AssetStatus.RETIRED) throw new IllegalStateException("Retired assets cannot be updated");
        this.unitId=unitId; this.name=name; this.category=category; this.serialNumber=serialNumber; this.location=location; this.installedOn=installedOn; this.updatedAt=now;
    }
    public void retire(Instant now) { if (status == AssetStatus.RETIRED) return; status=AssetStatus.RETIRED; updatedAt=now; }
    public UUID getId(){return id;} public UUID getPropertyId(){return propertyId;} public UUID getUnitId(){return unitId;} public String getName(){return name;} public String getCategory(){return category;} public String getSerialNumber(){return serialNumber;} public String getLocation(){return location;} public AssetStatus getStatus(){return status;} public LocalDate getInstalledOn(){return installedOn;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public long getVersion(){return version;}
}
