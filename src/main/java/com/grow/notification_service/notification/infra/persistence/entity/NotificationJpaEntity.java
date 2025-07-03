package com.grow.notification_service.notification.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificationId", nullable = false, updatable = false)
    private Long notificationId;

    @Column(name = "memberId", nullable = false, updatable = false)
    private Long memberId; // 알림 전송 대상자 ID

    @Column(name = "content", nullable = false, updatable = false)
    private String content; // 알림 내용

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 알림 생성 일자

    @Column(name = "isRead", nullable = false, updatable = false)
    private Boolean isRead; // 조회 여부

    @Column(name = "notificationType", nullable = false, updatable = false)
    private NotificationType notificationType; // 알림 타입

    @Builder
    public NotificationJpaEntity(Long memberId,
                                 String content,
                                 LocalDateTime createdAt,
                                 Boolean isRead,
                                 NotificationType notificationType
    ) {
        this.memberId = memberId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.notificationType = notificationType;
    }
}
