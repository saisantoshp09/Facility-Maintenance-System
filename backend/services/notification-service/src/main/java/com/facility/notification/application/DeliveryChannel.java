package com.facility.notification.application;

import com.facility.notification.domain.Notification;

/** Outbound delivery port (email, push, SMS...). Implementations throw DeliveryException on failure. */
public interface DeliveryChannel {
    void deliver(Notification notification) throws DeliveryException;
}
