package com.grow.notification_service.notification.domain.model;

import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
public class Notification {

    private Long notificationId;
    private Long memberId; // 알림 전송 대상자 ID
    private String content; // 알림 내용
    private LocalDateTime createdAt; // 알림 생성 일자
    private Boolean isRead; // 조회 여부
    private NotificationType notificationType; // 알림 타입
    private Boolean isSent; // 알림 전송 여부

    private Notification() {} // 직접 호출 방지

    // 생성 용도 static 메서드: 새로운 알림 객체 생성 (기본값 설정)
    public static Notification create(Long memberId,
                                      String content,
                                      Clock createdAt,
                                      NotificationType notificationType
    ) {
        Notification notification = new Notification();
        notification.notificationId = null; // DB 저장 후 자동 생성
        notification.memberId = memberId;
        notification.content = content;
        notification.isRead = false; // 기본은 읽지 않음
        notification.notificationType = notificationType;
        notification.isSent = false; // 기본은 전송하지 않음

        if (createdAt != null) {
            notification.createdAt = LocalDateTime.now(createdAt);
        } else {
            notification.createdAt = LocalDateTime.now();
        }

        return notification;
    }

    // 조회 용도 static 메서드: 기존 데이터로부터 알림 객체 생성 (모든 필드 직접 설정)
    public static Notification of(Long notificationId,
                                  Long memberId,
                                  String content,
                                  LocalDateTime createdAt,
                                  Boolean isRead,
                                  NotificationType notificationType,
                                  Boolean isSent
    ) {
        Notification notification = new Notification();
        notification.notificationId = notificationId;
        notification.memberId = memberId;
        notification.content = content;
        notification.createdAt = createdAt;
        notification.isRead = isRead;
        notification.notificationType = notificationType;
        notification.isSent = isSent;

        return notification;
    }
}
