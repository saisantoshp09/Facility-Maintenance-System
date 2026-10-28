package com.facility.asset.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name="service_records")
public class ServiceRecord {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id=UUID.randomUUID();
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="asset_id",nullable=false,columnDefinition="BINARY(16)") private UUID assetId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="task_id",columnDefinition="BINARY(16)") private UUID taskId;
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="performed_by",nullable=false,columnDefinition="BINARY(16)") private UUID performedBy;
    @Column(name="performed_on",nullable=false) private LocalDate performedOn;
    @Column(nullable=false,length=1000) private String summary;
    @Column(precision=12,scale=2) private BigDecimal cost;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected ServiceRecord() {}
    public ServiceRecord(UUID assetId,UUID taskId,UUID performedBy,LocalDate performedOn,String summary,BigDecimal cost,Instant createdAt){this.assetId=assetId;this.taskId=taskId;this.performedBy=performedBy;this.performedOn=performedOn;this.summary=summary;this.cost=cost;this.createdAt=createdAt;}
    public UUID getId(){return id;} public UUID getAssetId(){return assetId;} public UUID getTaskId(){return taskId;} public UUID getPerformedBy(){return performedBy;} public LocalDate getPerformedOn(){return performedOn;} public String getSummary(){return summary;} public BigDecimal getCost(){return cost;} public Instant getCreatedAt(){return createdAt;}
}
