package com.facility.asset.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.facility.asset.api.dto.AssetDtos.*;
import com.facility.asset.api.dto.PageResponse;
import com.facility.asset.application.AssetService;
import com.facility.asset.application.Caller;
import com.facility.asset.domain.Asset;
import com.facility.asset.domain.AssetStatus;
import com.facility.asset.domain.MaintenanceTask;
import com.facility.asset.domain.TaskStatus;
import jakarta.validation.Valid;

@RestController
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service){this.service=service;}
    @PostMapping("/api/v1/assets") ResponseEntity<AssetView> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AssetRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(AssetView.from(service.create(Caller.from(jwt),request)));}
    @GetMapping("/api/v1/assets") PageResponse<AssetView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID propertyId,@RequestParam(required=false) AssetStatus status,@RequestParam(required=false) String category,Pageable pageable){return PageResponse.from(service.list(Caller.from(jwt),propertyId,status,category,pageable).map(AssetView::from));}
    @GetMapping("/api/v1/assets/{id}") AssetView get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return AssetView.from(service.get(Caller.from(jwt),id));}
    @PutMapping("/api/v1/assets/{id}") AssetView update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody AssetUpdateRequest request){return AssetView.from(service.update(Caller.from(jwt),id,request));}
    @PostMapping("/api/v1/assets/{id}/retire") AssetView retire(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return AssetView.from(service.retire(Caller.from(jwt),id));}
    @PostMapping("/api/v1/assets/{id}/schedules") ResponseEntity<ScheduleView> createSchedule(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ScheduleRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(ScheduleView.from(service.createSchedule(Caller.from(jwt),id,request)));}
    @GetMapping("/api/v1/assets/{id}/schedules") List<ScheduleView> schedules(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.listSchedules(Caller.from(jwt),id).stream().map(ScheduleView::from).toList();}
    @PatchMapping("/api/v1/schedules/{sid}/deactivate") ScheduleView deactivate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID sid){return ScheduleView.from(service.deactivateSchedule(Caller.from(jwt),sid));}
    @PostMapping("/api/v1/assets/{id}/service-records") ResponseEntity<ServiceRecordView> record(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ServiceRecordRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(ServiceRecordView.from(service.recordService(Caller.from(jwt),id,request)));}
    @GetMapping("/api/v1/assets/{id}/service-records") PageResponse<ServiceRecordView> records(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,Pageable pageable){return PageResponse.from(service.listRecords(Caller.from(jwt),id,pageable).map(ServiceRecordView::from));}
    @GetMapping("/api/v1/tasks") PageResponse<TaskView> tasks(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID propertyId,@RequestParam(required=false) TaskStatus status,@RequestParam(required=false) Boolean overdue,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dueBefore,Pageable pageable){return PageResponse.from(service.listTasks(Caller.from(jwt),propertyId,status,overdue,dueBefore,pageable).map(TaskView::from));}
    @GetMapping("/api/v1/tasks/due") List<TaskView> due(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID propertyId,@RequestParam(defaultValue="7") int withinDays){return service.dueSoon(Caller.from(jwt),propertyId,withinDays).stream().map(TaskView::from).toList();}
}
