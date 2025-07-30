package com.grow.notification_service.notification.application.event;

import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <h2>알림 저장 후 발행되는 커스텀 이벤트 클래스</h2>
 * Spring의 ApplicationEvent를 상속받아 NotificationRequestDto를 포함하며,
 * 이벤트 리스너에서 이를 수신하여 후속 처리(SSE 알림 전송)를 수행합니다.
 *
 * <p>@Getter를 사용하여 dto 필드에 대한 getter 메서드를 제공합니다.
 * 이 이벤트는 NotificationServiceImpl의 processNotification 메서드에서 발행됩니다.
 *
 * <p><b>주요 용도:</b> 알림 DB 저장 후 실시간 푸시 알림을 트리거하기 위한 이벤트.
 *
 * <p><b>주의:</b> source는 이벤트 발행 객체(예: 서비스 인스턴스)를 나타내며,
 * dto는 null이 아닌 유효한 NotificationRequestDto 인스턴스여야 합니다.
 */
@Getter
public class NotificationSavedEvent extends ApplicationEvent {
    private final NotificationRequestDto dto;

    public NotificationSavedEvent(Object source, NotificationRequestDto dto) {
        super(source);
        this.dto = dto;
    }
}