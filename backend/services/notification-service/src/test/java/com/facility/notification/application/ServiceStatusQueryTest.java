package com.facility.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceStatusQueryTest {
    @Test
    void returnsServiceNameAndUpStatus() {
        ServiceStatusQuery query = new ServiceStatusQuery("notification-service");

        assertThat(query.getStatus().service()).isEqualTo("notification-service");
        assertThat(query.getStatus().status()).isEqualTo("UP");
    }
}
