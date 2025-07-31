package com.grow.notification_service.notification.application;

import com.grow.notification_service.notification.application.event.NotificationSavedEvent;
import com.grow.notification_service.notification.application.exception.SseException;
import com.grow.notification_service.notification.application.sse.SseSendService;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;
import com.grow.notification_service.notification.infra.persistence.repository.NotificationJpaRepository;
import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@RecordApplicationEvents // 이벤트 기록
class NotificationServiceImplTest {

    @Autowired
    private NotificationServiceImpl notificationService;

    @MockitoSpyBean
    private SseSendService sseSendService;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private ApplicationEvents events;  // 기록된 이벤트 확인을 위한 객체

    @Test
    @DisplayName("DB에 알림 저장 후 NotificationSavedEvent가 정상적으로 발행되고 SSE 알림이 비동기적으로 전송되는지 확인")
    void testEventPublishingAndSseSendingOnSave() {
        // given: SSE 연결 설정 (emitter 생성 및 등록)
        Long memberId = 1L;
        sseSendService.subscribe(memberId);  // SSE 연결 구독

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(memberId)
                .notificationType(NotificationType.MATCHING_SUCCESS)
                .content("테스트 알림 내용")
                .build();

        // when: 알림 처리 (DB 저장 및 이벤트 발행)
        notificationService.processNotification(request);

        // then: DB에 저장되었는지 확인
        List<NotificationJpaEntity> savedNotifications = notificationJpaRepository.findAll();
        assertThat(savedNotifications).hasSize(1);
        assertThat(savedNotifications.getFirst().getContent()).isEqualTo("테스트 알림 내용");

        // 이벤트가 발행되었는지 확인 (기록된 이벤트 사용)
        long eventCount = events.stream(NotificationSavedEvent.class).count();
        assertThat(eventCount).isEqualTo(1);

        // SSE 전송 메서드가 비동기적으로 호출되었는지 Spy로 확인 (ArgumentCaptor로 인자 검증)
        ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(sseSendService, timeout(5000).times(1))  // 비동기 대기
                .sendNotification(memberIdCaptor.capture(), typeCaptor.capture(), messageCaptor.capture());

        assertThat(memberIdCaptor.getValue()).isEqualTo(memberId);
        assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.MATCHING_SUCCESS);
        assertThat(messageCaptor.getValue()).isEqualTo("테스트 알림 내용");
    }

    @Test
    @DisplayName("SSE 연결(emitter)이 없을 때 이벤트 발행 시 SseException이 발생하고 알림 전송이 실패하는지 확인")
    void testEventPublishingFailureWithoutEmitter() {
        // given: SSE 연결 없음 (emitter 미등록)
        Long memberId = 1L;
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
                1L,
                NotificationType.MATCHING_SUCCESS,
                "테스트 알림 내용")
        );
    }

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