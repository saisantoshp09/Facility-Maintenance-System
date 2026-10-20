package com.facility.asset.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceStatusQueryTest {
    @Test
    void returnsServiceNameAndUpStatus() {
        ServiceStatusQuery query = new ServiceStatusQuery("asset-service");

        assertThat(query.getStatus().service()).isEqualTo("asset-service");
        assertThat(query.getStatus().status()).isEqualTo("UP");
    }
}
