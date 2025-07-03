package com.grow.notification_service.notification.infra.persistence.mapper;

import com.grow.notification_service.notification.domain.model.Notification;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    // 엔티티 -> 도메인 (조회)
    public Notification toDomain(NotificationJpaEntity entity) {
        return new Notification(
                entity.getNotificationId(),
                entity.getMemberId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getIsRead(),
                entity.getNotificationType()
        );
    }

    // 도메인 -> 엔티티 (최초 저장)
    public NotificationJpaEntity toEntity(Notification notification) {
        return NotificationJpaEntity.builder()
                .memberId(notification.getMemberId())
                .content(notification.getContent())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getIsRead())
                .notificationType(notification.getNotificationType())
                .build();
    }
}
