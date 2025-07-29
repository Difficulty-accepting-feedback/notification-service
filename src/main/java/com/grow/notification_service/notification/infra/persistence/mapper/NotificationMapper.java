package com.grow.notification_service.notification.infra.persistence.mapper;

import com.grow.notification_service.notification.domain.model.Notification;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity;
import org.springframework.stereotype.Component;

import static com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity.*;

@Component
public class NotificationMapper {

    /**
     * 엔티티 → 도메인 변환 (toDomain)
     * DB로부터 조회한 데이터를 도메인 객체로 가져와 비즈니스 로직에서 사용
     */
    public Notification toDomain(NotificationJpaEntity entity) {
        return Notification.of(
                entity.getNotificationId(),
                entity.getMemberId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getIsRead(),
                entity.getNotificationType(),
                entity.getIsSent()
        );
    }

    /**
     * 도메인 → 엔티티 변환(toEntity)
     * 새로운 도메인 객체를 생성하거나 변경된 도메인 상태를 DB에 반영
     */
    public NotificationJpaEntity toEntity(Notification domain) {
        NotificationJpaEntityBuilder builder = builder()
                .memberId(domain.getMemberId())
                .content(domain.getContent())
                .createdAt(domain.getCreatedAt())
                .isRead(domain.getIsRead())
                .notificationType(domain.getNotificationType())
                .isSent(domain.getIsSent());

        // ID 조건 설정: null이 아니면 업데이트용으로 ID 추가
        // null 일 경우에는 새로운 엔티티 생성
        if (domain.getNotificationId() != null) {
            builder.notificationId(domain.getNotificationId());
        }

        return builder.build();
    }
}
