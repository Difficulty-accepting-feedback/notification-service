package com.grow.notification_service.notification.application.sse;

import com.grow.notification_service.notification.application.event.NotificationSavedEvent;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseNotificationService {
    SseEmitter subscribe(Long memberId);
    void sendNotification(Long memberId,
                          NotificationType notificationType,
                          String message);
    void sendNotification(NotificationSavedEvent event);
}
