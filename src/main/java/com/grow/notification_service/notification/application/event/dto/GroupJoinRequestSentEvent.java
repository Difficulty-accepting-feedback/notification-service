package com.grow.notification_service.notification.application.event.dto;

import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequestSentEvent {

    private Long memberId; // 알림을 받는 사람의 아이디
    private String message; // 알림 내용
    private NotificationType notificationType; // 알림 타입 (그룹 참여 요청, 그룹 참여 승인, 그룹 참여 거절)
}