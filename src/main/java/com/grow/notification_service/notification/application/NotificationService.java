package com.grow.notification_service.notification.application;

import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;

public interface NotificationService {
    void processNotification(NotificationRequestDto requestDto);
}