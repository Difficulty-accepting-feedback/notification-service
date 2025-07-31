package com.grow.notification_service.notification.application.sse;

import com.grow.notification_service.notification.application.event.NotificationSavedEvent;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseSendService {
    SseEmitter subscribe(Long memberId);
    void sendNotification(Long memberId,
                          NotificationType notificationType,
                          String message);
    void handleNotificationSavedEvent(NotificationSavedEvent event);
}
