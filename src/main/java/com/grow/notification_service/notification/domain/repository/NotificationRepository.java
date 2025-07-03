package com.grow.notification_service.notification.domain.repository;

import com.grow.notification_service.notification.domain.model.Notification;

public interface NotificationRepository {
    Notification save(Notification notification);
}
