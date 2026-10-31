package com.facility.identity.api.dto;

import java.util.Set;

import com.facility.identity.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotEmpty Set<Role> roles) {
}
