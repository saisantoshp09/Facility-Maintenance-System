package com.facility.notification.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.facility.notification.api.dto.NotificationDtos.AttemptResponse;
import com.facility.notification.api.dto.NotificationDtos.NotificationResponse;
import com.facility.notification.api.dto.NotificationDtos.UnreadCountResponse;
import com.facility.notification.api.dto.PageResponse;
import com.facility.notification.application.InvalidRequestException;
import com.facility.notification.application.NotificationService;
import com.facility.notification.domain.NotificationStatus;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/notifications")
    PageResponse<NotificationResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(service.list(userId(jwt), unread, status, pageable), NotificationResponse::from);
    }

    @PostMapping("/notifications/{id}/read")
    NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return NotificationResponse.from(service.markRead(userId(jwt), id));
    }

    @GetMapping("/notifications/unread-count")
    UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCountResponse(service.unreadCount(userId(jwt)));
    }

    @GetMapping("/admin/notifications/{id}/attempts")
    @ResponseStatus(HttpStatus.OK)
    List<AttemptResponse> attempts(@PathVariable UUID id) {
        return service.attemptsOf(id).stream().map(AttemptResponse::from).toList();
    }

    private static UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidRequestException("Token subject is not a valid user id");
        }
    }
}
