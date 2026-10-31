package com.facility.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceStatusQueryTest {
    @Test
    void returnsServiceNameAndUpStatus() {
        ServiceStatusQuery query = new ServiceStatusQuery("identity-service");

        assertThat(query.getStatus().service()).isEqualTo("identity-service");
        assertThat(query.getStatus().status()).isEqualTo("UP");
    }
}
