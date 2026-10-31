package com.facility.notification.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facility.notification.api.dto.NotificationDtos.EventRequest;
import com.facility.notification.api.dto.NotificationDtos.EventResponse;
import com.facility.notification.application.EventIngestionService;

import jakarta.validation.Valid;

/** Service-to-service endpoint; authenticated by InternalApiKeyFilter, not by JWT. */
@RestController
@RequestMapping("/internal/v1/events")
public class InternalEventController {
    private final EventIngestionService ingestion;

    public InternalEventController(EventIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping
    ResponseEntity<EventResponse> ingest(@Valid @RequestBody EventRequest request) {
        var result = ingestion.ingest(new EventIngestionService.Command(request.eventId(), request.type(),
                request.occurredAt(), request.recipientUserIds(), request.subject(), request.message(),
                request.referenceType(), request.referenceId()));
        return result.accepted()
                ? ResponseEntity.status(HttpStatus.ACCEPTED).body(new EventResponse("ACCEPTED", result.notifications()))
                : ResponseEntity.ok(new EventResponse("DUPLICATE", null));
    }
}
