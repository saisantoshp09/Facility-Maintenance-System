package com.facility.maintenance.application;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.facility.maintenance.domain.OutboxEvent;
import com.facility.maintenance.domain.OutboxEventRepository;
import com.facility.maintenance.domain.OutboxStatus;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final OutboxEventRepository events;
    private final NotificationPublisher publisher;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;
    private final String apiKey;
    private volatile boolean missingKeyLogged;

    public OutboxDispatcher(OutboxEventRepository events, NotificationPublisher publisher, Clock clock,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-attempts:5}") int maxAttempts,
            @Value("${app.notification-service.api-key:}") String apiKey) {
        this.events = events;
        this.publisher = publisher;
        this.clock = clock;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.apiKey = apiKey;
    }

    @Scheduled(fixedDelayString = "${app.outbox.interval:5s}")
    public void dispatchDueEvents() {
        if (apiKey == null || apiKey.isBlank()) {
            if (!missingKeyLogged) {
                log.warn("Outbox dispatch is paused because INTERNAL_API_KEY is not configured");
                missingKeyLogged = true;
            }
            return;
        }
        List<OutboxEvent> due = events.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, clock.instant(), PageRequest.of(0, batchSize));
        for (OutboxEvent event : due) {
            try {
                publisher.publish(event.getPayload());
                event.markSent(clock.instant());
                events.save(event);
            } catch (Exception ex) {
                event.markFailedAttempt(ex.getMessage(), clock.instant(), maxAttempts);
                events.save(event);
                log.warn("Notification event {} publish failed (attempt {}, status {})", event.getId(),
                        event.getAttempts(), event.getStatus());
            }
        }
    }
}
