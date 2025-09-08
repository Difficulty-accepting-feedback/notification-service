package com.grow.notification_service.notification.application.service.impl;

import com.grow.notification_service.notification.application.event.dto.NotificationSavedEvent;
import com.grow.notification_service.notification.application.service.NotificationService;
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
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@RecordApplicationEvents // 이벤트 기록
class NotificationServiceImplTest {

    @Autowired
    private NotificationService notificationService;

    @MockitoSpyBean
    private SseSendService sseSendService;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private ApplicationEvents events;  // 기록된 이벤트 확인을 위한 객체

    @Test
    @DisplayName("DB 저장 후 NotificationSavedEvent 발행 및 SSE 전송 비동기 검증")
    void testEventPublishingAndSseSendingOnSave() {
        // given
        Long memberId = 1L;
        sseSendService.subscribe(memberId); // 테스트용 emitter 등록

        NotificationRequestDto request = NotificationRequestDto.builder()
            .memberId(memberId)
            .notificationType(NotificationType.MATCHING_SUCCESS)
            .content("테스트 알림 내용")
            .build();

        // when (비동기 처리 시작)
        notificationService.processNotification(request);

        // 1) 이벤트 발행 도달까지 대기 (저장 완료 시점도 보장됨)
        waitUntilEventCount(events, NotificationSavedEvent.class, 1, 5_000);

        // 2) SSE 전송 호출까지 대기 + 인자 검증
        ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(sseSendService, timeout(5_000).times(1))
            .sendNotification(memberIdCaptor.capture(), typeCaptor.capture(), messageCaptor.capture());

        assertThat(memberIdCaptor.getValue()).isEqualTo(memberId);
        assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.MATCHING_SUCCESS);
        assertThat(messageCaptor.getValue()).isEqualTo("테스트 알림 내용");

        // 3) DB 반영까지 대기 후 검증 (비동기 커밋 타이밍 대비)
        waitUntilRepoCount(notificationJpaRepository, 1L, 5_000);

        List<NotificationJpaEntity> saved = notificationJpaRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getMemberId()).isEqualTo(memberId);
        assertThat(saved.get(0).getNotificationType()).isEqualTo(NotificationType.MATCHING_SUCCESS);
        assertThat(saved.get(0).getContent()).isEqualTo("테스트 알림 내용");
    }

    /* ---------- helpers ---------- */

    private static <T> void waitUntilEventCount(
        ApplicationEvents events, Class<T> type, int expected, long timeoutMillis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (events.stream(type).count() >= expected) return;
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        long finalCount = events.stream(type).count();
        assertThat(finalCount)
            .withFailMessage("이벤트 %s 개수(%d)가 제한 시간 내 기대치(%d)에 도달하지 못했습니다.",
                type.getSimpleName(), finalCount, expected)
            .isGreaterThanOrEqualTo(expected);
    }

    private static void waitUntilRepoCount(
        NotificationJpaRepository repo, long expected, long timeoutMillis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (repo.count() >= expected) return;
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        long finalCount = repo.count();
        assertThat(finalCount)
            .withFailMessage("DB row 수(%d)가 제한 시간 내 기대치(%d)에 도달하지 못했습니다.",
                finalCount, expected)
            .isGreaterThanOrEqualTo(expected);
    }
}