package com.facility.asset.api.dto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.facility.asset.domain.Asset;
import com.facility.asset.domain.MaintenanceSchedule;
import com.facility.asset.domain.MaintenanceTask;
import com.facility.asset.domain.ServiceRecord;
public final class AssetDtos {
    private AssetDtos() {}
    public record AssetRequest(@NotNull UUID propertyId, UUID unitId, @NotBlank @Size(max=160) String name,
            @NotBlank @Size(max=80) String category, @Size(max=120) String serialNumber,
            @NotBlank @Size(max=200) String location, LocalDate installedOn) {}
    public record AssetUpdateRequest(UUID unitId, @NotBlank @Size(max=160) String name,
            @NotBlank @Size(max=80) String category, @Size(max=120) String serialNumber,
            @NotBlank @Size(max=200) String location, LocalDate installedOn) {}
    public record AssetView(UUID id,UUID propertyId,UUID unitId,String name,String category,String serialNumber,String location,
            String status,LocalDate installedOn,Instant createdAt,Instant updatedAt,long version) {
        public static AssetView from(Asset a){return new AssetView(a.getId(),a.getPropertyId(),a.getUnitId(),a.getName(),a.getCategory(),a.getSerialNumber(),a.getLocation(),a.getStatus().name(),a.getInstalledOn(),a.getCreatedAt(),a.getUpdatedAt(),a.getVersion());}
    }
    public record ScheduleRequest(@NotBlank @Size(max=160) String title,@NotBlank @Size(max=1000) String description,
            @Positive int intervalDays,@NotNull LocalDate startDate) {}
    public record ScheduleView(UUID id,UUID assetId,String title,String description,int intervalDays,LocalDate startDate,
            LocalDate nextDueDate,LocalDate lastCompletedDate,boolean active,long version) {
        public static ScheduleView from(MaintenanceSchedule s){return new ScheduleView(s.getId(),s.getAssetId(),s.getTitle(),s.getDescription(),s.getIntervalDays(),s.getStartDate(),s.getNextDueDate(),s.getLastCompletedDate(),s.isActive(),s.getVersion());}
    }
    public record TaskView(UUID id,UUID scheduleId,UUID assetId,UUID propertyId,LocalDate dueDate,String status,Instant completedAt) {
        public static TaskView from(MaintenanceTask t){return new TaskView(t.getId(),t.getScheduleId(),t.getAssetId(),t.getPropertyId(),t.getDueDate(),t.getStatus().name(),t.getCompletedAt());}
    }
    public record ServiceRecordRequest(UUID taskId,@NotNull LocalDate performedOn,@NotBlank @Size(max=1000) String summary,
            @DecimalMin("0.00") java.math.BigDecimal cost) {}
    public record ServiceRecordView(UUID id,UUID assetId,UUID taskId,UUID performedBy,LocalDate performedOn,String summary,
            java.math.BigDecimal cost,Instant createdAt) {
        public static ServiceRecordView from(ServiceRecord r){return new ServiceRecordView(r.getId(),r.getAssetId(),r.getTaskId(),r.getPerformedBy(),r.getPerformedOn(),r.getSummary(),r.getCost(),r.getCreatedAt());}
    }
}
