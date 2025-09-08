package com.grow.notification_service.notification.application.event.consumer;

import com.grow.notification_service.global.util.JsonUtils;
import com.grow.notification_service.notification.application.event.dto.GroupJoinRequestSentEvent;
import com.grow.notification_service.notification.application.service.NotificationService;
import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupJoinRequestNotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "group.join-request.notification",
            groupId = "group.join.notification-service",
            concurrency = "3"
    )
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    @Transactional
    public void consume(String message) {
        // Json -> 객체로 변환
        GroupJoinRequestSentEvent sentEvent = JsonUtils.fromJsonString(message, GroupJoinRequestSentEvent.class);

        // 프론트엔드로 알림 전송 + DB에 알림 저장
        notificationService.processNotification(
                NotificationRequestDto.builder()
                        .memberId(sentEvent.getMemberId())
                        .content(sentEvent.getMessage())
                        .timestamp(LocalDateTime.now())
                        .notificationType(sentEvent.getNotificationType())
                        .build()
        );
    }
}
