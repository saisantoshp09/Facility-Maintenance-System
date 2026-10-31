package com.facility.identity.api.dto;

import java.util.Set;

import com.facility.identity.domain.Role;

import jakarta.validation.constraints.NotEmpty;

public record UpdateRolesRequest(@NotEmpty Set<Role> roles) {
}
