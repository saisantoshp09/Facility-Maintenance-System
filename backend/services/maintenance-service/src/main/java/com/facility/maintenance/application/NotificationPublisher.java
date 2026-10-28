package com.facility.maintenance.application;

public interface NotificationPublisher {
    void publish(String payload) throws Exception;
}
