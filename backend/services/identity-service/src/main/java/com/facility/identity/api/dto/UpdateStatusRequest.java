package com.facility.identity.api.dto;

import com.facility.identity.domain.AccountStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull AccountStatus status) {
}
