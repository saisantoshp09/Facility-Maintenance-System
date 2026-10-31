package com.facility.asset.domain;

import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity @Table(name="maintenance_schedules")
public class MaintenanceSchedule {
    @Id @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id=UUID.randomUUID();
    @JdbcTypeCode(SqlTypes.BINARY) @Column(name="asset_id", nullable=false, columnDefinition="BINARY(16)") private UUID assetId;
    @Column(nullable=false,length=160) private String title;
    @Column(nullable=false,length=1000) private String description;
    @Column(name="interval_days",nullable=false) private int intervalDays;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="next_due_date",nullable=false) private LocalDate nextDueDate;
    @Column(name="last_completed_date") private LocalDate lastCompletedDate;
    @Column(nullable=false) private boolean active=true;
    @Version @Column(nullable=false) private long version;
    protected MaintenanceSchedule() {}
    public MaintenanceSchedule(UUID assetId,String title,String description,int intervalDays,LocalDate startDate) {
        if(intervalDays<1) throw new IllegalArgumentException("intervalDays must be at least 1");
        this.assetId=assetId; this.title=title; this.description=description; this.intervalDays=intervalDays; this.startDate=startDate; this.nextDueDate=startDate;
    }
    public LocalDate completeOn(LocalDate completionDate) {
        if(!active) throw new IllegalStateException("Inactive schedule cannot be completed");
        if(completionDate.isBefore(nextDueDate)) { /* early completion is permitted */ }
        lastCompletedDate=completionDate; nextDueDate=completionDate.plusDays(intervalDays); return nextDueDate;
    }
    public void deactivate(){active=false;}
    public UUID getId(){return id;} public UUID getAssetId(){return assetId;} public String getTitle(){return title;} public String getDescription(){return description;} public int getIntervalDays(){return intervalDays;} public LocalDate getStartDate(){return startDate;} public LocalDate getNextDueDate(){return nextDueDate;} public LocalDate getLastCompletedDate(){return lastCompletedDate;} public boolean isActive(){return active;} public long getVersion(){return version;}
}
