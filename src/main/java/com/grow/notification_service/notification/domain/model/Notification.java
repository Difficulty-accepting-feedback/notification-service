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

    public Notification(Long memberId,
                        String content,
                        Clock createdAt,
                        NotificationType notificationType
    ) {
        this.notificationId = null; // DB 저장 후 자동 생성
        this.memberId = memberId;
        this.content = content;
        this.isRead = false; // 기본은 읽지 않음
        this.notificationType = notificationType;

        if(createdAt != null) {
            this.createdAt = LocalDateTime.now(createdAt);
        } else  {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Notification(Long notificationId,
                        Long memberId, String content,
                        LocalDateTime createdAt,
                        Boolean isRead,
                        NotificationType notificationType
    ) {
        this.notificationId = notificationId;
        this.memberId = memberId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.notificationType = notificationType;
    }
}
