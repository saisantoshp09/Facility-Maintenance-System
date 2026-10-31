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

@Entity @Table(name="maintenance_tasks")
public class MaintenanceTask {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id=UUID.randomUUID();
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="schedule_id",nullable=false,columnDefinition="BINARY(16)") private UUID scheduleId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="asset_id",nullable=false,columnDefinition="BINARY(16)") private UUID assetId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="property_id",nullable=false,columnDefinition="BINARY(16)") private UUID propertyId;
    @Column(name="due_date",nullable=false) private LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private TaskStatus status=TaskStatus.PENDING;
    @Column(name="completed_at") private Instant completedAt;
    protected MaintenanceTask() {}
    public MaintenanceTask(UUID scheduleId,UUID assetId,UUID propertyId,LocalDate dueDate){this.scheduleId=scheduleId;this.assetId=assetId;this.propertyId=propertyId;this.dueDate=dueDate;}
    public void complete(Instant when){if(status!=TaskStatus.PENDING) throw new IllegalStateException("Only pending tasks can be completed");status=TaskStatus.COMPLETED;completedAt=when;}
    public void cancel(){if(status==TaskStatus.PENDING)status=TaskStatus.CANCELLED;}
    public UUID getId(){return id;} public UUID getScheduleId(){return scheduleId;} public UUID getAssetId(){return assetId;} public UUID getPropertyId(){return propertyId;} public LocalDate getDueDate(){return dueDate;} public TaskStatus getStatus(){return status;} public Instant getCompletedAt(){return completedAt;}
}


