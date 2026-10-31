package com.facility.maintenance.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.facility.maintenance.api.dto.PageResponse;
import com.facility.maintenance.api.dto.WorkOrderDtos.CommentRequest;
import com.facility.maintenance.api.dto.WorkOrderDtos.CommentResponse;
import com.facility.maintenance.api.dto.WorkOrderDtos.CreateRequest;
import com.facility.maintenance.api.dto.WorkOrderDtos.DueDateRequest;
import com.facility.maintenance.api.dto.WorkOrderDtos.HistoryResponse;
import com.facility.maintenance.api.dto.WorkOrderDtos.TransitionRequest;
import com.facility.maintenance.api.dto.WorkOrderDtos.WorkOrderResponse;
import com.facility.maintenance.application.Caller;
import com.facility.maintenance.application.WorkOrderService;
import com.facility.maintenance.domain.Priority;
import com.facility.maintenance.domain.WorkOrder;
import com.facility.maintenance.domain.WorkOrderStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/work-orders")
public class WorkOrderController {
    private final WorkOrderService service;
    private final Clock clock;

    public WorkOrderController(WorkOrderService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping
    ResponseEntity<WorkOrderResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(view(service.create(Caller.from(jwt), request)));
    }

    @GetMapping
    PageResponse<WorkOrderResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID propertyId,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueBefore,
            Pageable pageable) {
        Instant now = clock.instant();
        return PageResponse.from(service.list(Caller.from(jwt), propertyId, status, priority, assigneeId, overdue,
                dueBefore, pageable), w -> WorkOrderResponse.from(w, now));
    }

    @GetMapping("/{id}")
    WorkOrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return view(service.get(Caller.from(jwt), id));
    }

    /** Single state-machine entry point: approve, reject, assign, start, hold, resume, resolve, close. */
    @PostMapping("/{id}/transitions")
    WorkOrderResponse transition(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request) {
        return view(service.transition(Caller.from(jwt), id, request));
    }

    @PatchMapping("/{id}/due-date")
    WorkOrderResponse dueDate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody DueDateRequest request) {
        return view(service.changeDueDate(Caller.from(jwt), id, request.dueAt()));
    }

    @PostMapping("/{id}/comments")
    ResponseEntity<CommentResponse> addComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentResponse.from(service.addComment(Caller.from(jwt), id, request.body())));
    }

    @GetMapping("/{id}/comments")
    PageResponse<CommentResponse> comments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            Pageable pageable) {
        return PageResponse.from(service.listComments(Caller.from(jwt), id, pageable), CommentResponse::from);
    }

    @GetMapping("/{id}/history")
    List<HistoryResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.listHistory(Caller.from(jwt), id).stream().map(HistoryResponse::from).toList();
    }

    private WorkOrderResponse view(WorkOrder order) {
        return WorkOrderResponse.from(order, clock.instant());
    }
}
