package com.facility.notification.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.facility.notification.application.DeliveryChannel;
import com.facility.notification.application.DeliveryException;
import com.facility.notification.domain.Notification;

/** Local stand-in for a real channel: logs identifiers only (never content) and can fail for chosen types. */
@Component
public class MockDeliveryChannel implements DeliveryChannel {
    private static final Logger log = LoggerFactory.getLogger(MockDeliveryChannel.class);
    private final Set<String> failTypes;

    public MockDeliveryChannel(@Value("${app.delivery.mock-fail-types:}") String failTypes) {
        this.failTypes = Arrays.stream(failTypes.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void deliver(Notification notification) throws DeliveryException {
        if (failTypes.contains(notification.getType())) {
            throw new DeliveryException("Simulated delivery failure");
        }
        log.info("Mock delivery of notification {} to user {}", notification.getId(),
                notification.getRecipientUserId());
    }
}
