package com.facility.maintenance.api;

import com.facility.maintenance.application.ServiceStatusQuery;
import com.facility.maintenance.api.dto.ServiceStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class ServiceStatusController {
    private final ServiceStatusQuery serviceStatusQuery;

    public ServiceStatusController(ServiceStatusQuery serviceStatusQuery) {
        this.serviceStatusQuery = serviceStatusQuery;
    }

    @GetMapping
    public ServiceStatusResponse getStatus() {
        var status = serviceStatusQuery.getStatus();
        return new ServiceStatusResponse(status.service(), status.status());
    }
}
