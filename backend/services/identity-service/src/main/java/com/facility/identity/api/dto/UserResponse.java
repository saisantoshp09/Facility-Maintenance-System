package com.facility.identity.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.facility.identity.domain.AccountStatus;
import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccount;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        AccountStatus status,
        List<Role> roles,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getStatus(),
                user.getRoles().stream().sorted().toList(), user.getCreatedAt(), user.getUpdatedAt(),
                user.getLastLoginAt());
    }
}
