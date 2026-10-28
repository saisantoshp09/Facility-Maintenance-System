package com.facility.identity.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facility.identity.api.dto.CreateUserRequest;
import com.facility.identity.api.dto.PageResponse;
import com.facility.identity.api.dto.UpdateRolesRequest;
import com.facility.identity.api.dto.UpdateStatusRequest;
import com.facility.identity.api.dto.UserResponse;
import com.facility.identity.application.AuthenticationFailedException;
import com.facility.identity.application.NotFoundException;
import com.facility.identity.application.UserAdministrationService;
import com.facility.identity.domain.UserAccount;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserAdministrationService users;

    public UserController(UserAdministrationService users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        try {
            UserAccount user = users.getUser(actorId(jwt));
            if (!user.isActive()) {
                throw new AuthenticationFailedException();
            }
            return UserResponse.from(user);
        } catch (NotFoundException ex) {
            throw new AuthenticationFailedException();
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResponse<UserResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(users.listUsers(pageable), UserResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<UserResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserRequest request) {
        UserAccount user = users.createUser(actorId(jwt), request.email(), request.displayName(),
                request.password(), request.roles());
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId())).body(UserResponse.from(user));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse get(@PathVariable UUID userId) {
        return UserResponse.from(users.getUser(userId));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return UserResponse.from(users.changeStatus(actorId(jwt), userId, request.status()));
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse updateRoles(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @Valid @RequestBody UpdateRolesRequest request) {
        return UserResponse.from(users.replaceRoles(actorId(jwt), userId, request.roles()));
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
