package com.grow.notification_service.notification.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "notification")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType; // 알림 타입

    @Column(name = "isSent", nullable = false)
    private Boolean isSent; // 알림 전송 여부 -> 처음 저장 시에는 false로 설정
}
