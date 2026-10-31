package com.facility.notification.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.facility.notification.domain.DeliveryAttempt;
import com.facility.notification.domain.DeliveryAttemptRepository;
import com.facility.notification.domain.DeliveryOutcome;
import com.facility.notification.domain.Notification;
import com.facility.notification.domain.NotificationRepository;
import com.facility.notification.domain.NotificationStatus;

/** Outbox-style dispatcher: delivers PENDING notifications that are due, with retry and exponential backoff. */
@Service
public class DeliveryService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final NotificationRepository notifications;
    private final DeliveryAttemptRepository attempts;
    private final DeliveryChannel channel;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int maxAttempts;
    private final int batchSize;
    private final Duration backoff;

    public DeliveryService(NotificationRepository notifications, DeliveryAttemptRepository attempts,
            DeliveryChannel channel, TransactionTemplate tx, Clock clock,
            @Value("${app.delivery.max-attempts:3}") int maxAttempts,
            @Value("${app.delivery.batch-size:50}") int batchSize,
            @Value("${app.delivery.backoff:30s}") Duration backoff) {
        this.notifications = notifications;
        this.attempts = attempts;
        this.channel = channel;
        this.tx = tx;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
        this.backoff = backoff;
    }

    /** Processes one batch of due notifications and returns how many were attempted. */
    public int deliverDue() {
        List<java.util.UUID> due = notifications.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                NotificationStatus.PENDING, clock.instant(), PageRequest.of(0, batchSize))
                .stream().map(Notification::getId).toList();
        int processed = 0;
        for (java.util.UUID id : due) {
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> attempt(id)))) {
                    processed++;
                }
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.debug("Notification {} was handled concurrently", id);
            }
        }
        return processed;
    }

    private boolean attempt(java.util.UUID id) {
        Notification n = notifications.findById(id).orElse(null);
        Instant now = clock.instant();
        if (n == null || n.getStatus() != NotificationStatus.PENDING
                || (n.getNextAttemptAt() != null && n.getNextAttemptAt().isAfter(now))) {
            return false;
        }
        int attemptNo = n.getAttempts() + 1;
        try {
            channel.deliver(n);
            n.markSent();
            attempts.save(new DeliveryAttempt(id, attemptNo, DeliveryOutcome.SUCCESS, null, now));
        } catch (DeliveryException | RuntimeException ex) {
            Duration delay = backoff.multipliedBy(1L << Math.min(attemptNo - 1, 16));
            n.markFailedAttempt(maxAttempts, delay, now);
            attempts.save(new DeliveryAttempt(id, attemptNo, DeliveryOutcome.FAILURE, ex.getMessage(), now));
            log.warn("Delivery attempt {} failed for notification {} (status {})", attemptNo, id, n.getStatus());
        }
        notifications.save(n);
        return true;
    }
}
