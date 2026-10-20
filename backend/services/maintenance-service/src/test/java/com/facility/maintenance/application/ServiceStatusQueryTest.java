package com.facility.maintenance.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceStatusQueryTest {
    @Test
    void returnsServiceNameAndUpStatus() {
        ServiceStatusQuery query = new ServiceStatusQuery("maintenance-service");

        assertThat(query.getStatus().service()).isEqualTo("maintenance-service");
        assertThat(query.getStatus().status()).isEqualTo("UP");
    }
}
