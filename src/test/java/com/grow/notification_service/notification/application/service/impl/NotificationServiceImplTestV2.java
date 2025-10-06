package com.grow.notification_service.notification.application.service.impl;

import com.grow.notification_service.notification.application.exception.SseException;
import com.grow.notification_service.notification.application.service.NotificationService;
import com.grow.notification_service.notification.application.sse.SseSendService;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import com.grow.notification_service.notification.infra.persistence.repository.NotificationJpaRepository;
import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceImplTestV2 {

    @Autowired
    private NotificationService notificationService;

    @MockitoSpyBean
    private SseSendService sseSendService;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Test
    @Disabled("로컬 빌드 성공하나 깃허브 액션 오류")
    @DisplayName("SSE 연결(emitter)이 없을 때 이벤트 발행 시 SseException이 발생하고 알림 전송이 실패하는지 확인")
    void testEventPublishingFailureWithoutEmitter() {
        // given: SSE 연결 없음 (emitter 미등록)
        Long memberId = 10L;
        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(memberId)
                .notificationType(NotificationType.MATCHING_SUCCESS)
                .content("테스트 알림 내용")
                .build();

        // when & then: 알림 처리 시 예외 발생 확인
        notificationService.processNotification(request);  // DB 저장은 성공하지만 SSE 전송에서 예외

        // DB 저장은 성공
        List<NotificationJpaEntity> savedNotifications = notificationJpaRepository.findAll();
        assertThat(savedNotifications).hasSize(1);

        // 이벤트는 발행되었으나 SSE 전송에서 예외 발생
        assertThrows(SseException.class, () -> sseSendService.sendNotification(
                10L,
                NotificationType.MATCHING_SUCCESS,
                "테스트 알림 내용")
        );
    }
}