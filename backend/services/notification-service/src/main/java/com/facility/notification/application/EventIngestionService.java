package com.facility.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.facility.notification.domain.Notification;
import com.facility.notification.domain.NotificationRepository;
import com.facility.notification.domain.ProcessedEvent;
import com.facility.notification.domain.ProcessedEventRepository;

@Service
public class EventIngestionService {
    public record Command(UUID eventId, String type, Instant occurredAt, List<UUID> recipientUserIds,
            String subject, String message, String referenceType, UUID referenceId) {
    }

    /** accepted == false means the event id was already processed. */
    public record Result(boolean accepted, int notifications) {
    }

    private final ProcessedEventRepository processed;
    private final NotificationRepository notifications;
    private final TransactionTemplate tx;
    private final Clock clock;

    public EventIngestionService(ProcessedEventRepository processed, NotificationRepository notifications,
            TransactionTemplate tx, Clock clock) {
        this.processed = processed;
        this.notifications = notifications;
        this.tx = tx;
        this.clock = clock;
    }

    public Result ingest(Command command) {
        try {
            return tx.execute(status -> store(command));
        } catch (DataIntegrityViolationException ex) {
            // A concurrent request won the unique constraint on the event id.
            return new Result(false, 0);
        }
    }

    private Result store(Command command) {
        if (processed.existsById(command.eventId())) {
            return new Result(false, 0);
        }
        Instant now = clock.instant();
        processed.saveAndFlush(new ProcessedEvent(command.eventId(), now));
        var recipients = new LinkedHashSet<>(command.recipientUserIds());
        List<Notification> created = recipients.stream()
                .map(user -> new Notification(command.eventId(), user, command.type(), command.subject(),
                        command.message(), command.referenceType(), command.referenceId(), now))
                .toList();
        notifications.saveAll(created);
        return new Result(true, created.size());
    }
}
