package com.facility.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CorrelationIdFilterTest {
    @Test
    void acceptsSafeCorrelationId() {
        assertThat(CorrelationIdFilter.correlationIdFor("request-123")).isEqualTo("request-123");
    }

    @Test
    void replacesInvalidCorrelationId() {
        String generated = CorrelationIdFilter.correlationIdFor("invalid/id");

        assertThat(UUID.fromString(generated)).isNotNull();
    }
}
