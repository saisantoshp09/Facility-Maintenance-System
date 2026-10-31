package com.facility.notification.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.delivery.enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryScheduler {
    private final DeliveryService delivery;

    public DeliveryScheduler(DeliveryService delivery) {
        this.delivery = delivery;
    }

    @Scheduled(fixedDelayString = "${app.delivery.poll-interval:5s}", initialDelayString = "${app.delivery.poll-interval:5s}")
    void run() {
        delivery.deliverDue();
    }
}
