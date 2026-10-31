package com.facility.notification.application;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facility.notification.domain.DeliveryAttempt;
import com.facility.notification.domain.DeliveryAttemptRepository;
import com.facility.notification.domain.Notification;
import com.facility.notification.domain.NotificationRepository;
import com.facility.notification.domain.NotificationStatus;

@Service
public class NotificationService {
    private static final Set<String> SORTS = Set.of("createdAt", "status", "readAt");

    private final NotificationRepository notifications;
    private final DeliveryAttemptRepository attempts;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, DeliveryAttemptRepository attempts,
            Clock clock) {
        this.notifications = notifications;
        this.attempts = attempts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(UUID userId, boolean unreadOnly, NotificationStatus status, Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTS.contains(order.getProperty())) {
                throw new InvalidRequestException("Unsupported sort property: " + order.getProperty());
            }
        });
        Specification<Notification> spec = (root, query, cb) -> cb.equal(root.get("recipientUserId"), userId);
        if (unreadOnly) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("readAt")));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return notifications.findAll(spec, pageable);
    }

    @Transactional
    public Notification markRead(UUID userId, UUID id) {
        Notification n = notifications.findByIdAndRecipientUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (n.getReadAt() == null) {
            n.markRead(clock.instant());
        }
        return n;
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.countByRecipientUserIdAndReadAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAttempt> attemptsOf(UUID notificationId) {
        if (!notifications.existsById(notificationId)) {
            throw new NotFoundException("Notification not found");
        }
        return attempts.findByNotificationIdOrderByAttemptNoAsc(notificationId);
    }
}
