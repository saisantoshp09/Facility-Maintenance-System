package com.facility.property.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceStatusQueryTest {
    @Test
    void returnsServiceNameAndUpStatus() {
        ServiceStatusQuery query = new ServiceStatusQuery("property-service");

        assertThat(query.getStatus().service()).isEqualTo("property-service");
        assertThat(query.getStatus().status()).isEqualTo("UP");
    }
}
