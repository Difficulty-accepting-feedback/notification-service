
package com.grow.notification_service.notification.presentation.dto;

import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 전송을 위한 DTO 클래스.
 * 이 DTO는 알림 요청 데이터를 캡슐화하며, 멤버 ID, 내용, 타입, 타임스탬프, 재시도 횟수를 포함합니다.
 * 주로 NotificationServiceClient나 QueueService에서 사용되어 장애 시 재시도 로직을 지원합니다.
 *
 * <p>이 클래스는 Lombok의 @Getter와 @Builder를 사용하여 간편한 객체 생성과 접근을 제공합니다.
 * retryCount 필드는 알림 전송 실패 시 증가되며, 재시도 한계를 관리할 수 있습니다.</p>
 *
 * @see LocalDateTime
 */
@Getter
@Builder
public class NotificationRequestDto {

    /**
     * 알림을 받을 멤버의 고유 ID.
     */
    @NotNull
    private Long memberId;

    /**
     * 알림 내용 (텍스트 메시지).
     */
    @NotNull
    private String content;

    /**
     * 알림 타입 (예: "MATCH_SUCCESS" 등).
     */
    @NotNull
    private NotificationType notificationType;

    /**
     * 알림 생성 타임스탬프.
     */
    private LocalDateTime timestamp;
}
