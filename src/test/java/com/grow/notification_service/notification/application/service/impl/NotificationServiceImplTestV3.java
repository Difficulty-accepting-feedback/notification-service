package com.grow.notification_service.notification.application.service.impl;

import com.grow.notification_service.notification.application.service.NotificationService;
import com.grow.notification_service.notification.application.sse.SseSendService;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceImplTestV3 {

    @Autowired
    private NotificationService notificationService;

    @MockitoSpyBean
    private SseSendService sseSendService;

    @Test
    @DisplayName("이벤트 처리 메서드가 비동기(@Async)로 동작하는지 확인 (스레드 이름 검증)")
    void testAsyncEventHandling() {
        // given: SSE 연결 설정
        Long memberId = 1L;
        sseSendService.subscribe(memberId);

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(memberId)
                .notificationType(NotificationType.MATCHING_SUCCESS)
                .content("테스트 알림 내용")
                .build();

        // Spy로 sendNotification 메서드의 스레드 확인을 위한 doAnswer 사용
        doAnswer(invocation -> {
            String currentThreadName = Thread.currentThread().getName();
            assertThat(currentThreadName).startsWith("task-");  // @Async 기본 풀 이름 확인
            return invocation.callRealMethod();
        }).when(sseSendService).sendNotification(any(Long.class), any(NotificationType.class), any(String.class));

        // when: 알림 처리
        notificationService.processNotification(request);

        // then: 비동기 호출 확인 (verify로 메서드 호출)
        verify(sseSendService, timeout(5000).times(1))
                .sendNotification(any(Long.class), any(NotificationType.class), any(String.class));
    }
}