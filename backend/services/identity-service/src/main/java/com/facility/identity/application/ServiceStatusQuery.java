package com.facility.identity.application;

import com.facility.identity.domain.ServiceStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceStatusQuery {
    private final String serviceName;

    public ServiceStatusQuery(@Value("${spring.application.name}") String serviceName) {
        this.serviceName = serviceName;
    }

    public ServiceStatus getStatus() {
        return new ServiceStatus(serviceName, "UP");
    }
}
