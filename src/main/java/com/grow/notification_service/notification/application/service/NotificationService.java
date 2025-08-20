package com.grow.notification_service.notification.application.service;

import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;

public interface NotificationService {
    void processNotification(NotificationRequestDto requestDto);
}