package com.facility.property.api.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String correlationId,
        List<FieldIssue> fieldErrors) {

    public record FieldIssue(String field, String message) {
    }
}
