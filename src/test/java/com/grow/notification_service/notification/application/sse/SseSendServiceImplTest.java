package com.grow.notification_service.notification.application.sse;

import com.grow.notification_service.global.metrics.NotificationMetrics;
import com.grow.notification_service.notification.application.event.dto.NotificationSavedEvent;
import com.grow.notification_service.notification.application.exception.SseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.grow.notification_service.notification.application.exception.ErrorCode.SSE_NOT_CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseSendServiceImplTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private NotificationMetrics metrics;

    @InjectMocks
    private SseSendServiceImpl sseNotificationService;

    private Map<Long, SseEmitter> sseEmitters;

    @BeforeEach
    void setUp() {
        sseEmitters = new ConcurrentHashMap<>();
        // private 필드 주입
        ReflectionTestUtils.setField(sseNotificationService, "sseEmitters", sseEmitters);

        lenient().doAnswer(invocation -> null)
            .when(meterRegistry).gauge(anyString(), any(Map.class), any());

        lenient().doNothing().when(metrics).result(anyString(), any(String[].class));
    }

    @Test
    @DisplayName("SSE 연결 성공 테스트")
    void subscribe_success() throws Exception {
        Long memberId = 1L;

        SseEmitter result = sseNotificationService.subscribe(memberId);

        assertNotNull(result);
        assertTrue(sseEmitters.containsKey(memberId));
        assertEquals(result, sseEmitters.get(memberId));
        assertThat(sseEmitters.get(memberId)).isInstanceOf(SseEmitter.class);
    }

    @Test
    @DisplayName("SSE 연결 실패 테스트 - IOException 발생 시 SseException 던짐")
    void subscribe_failureWhenIOException() throws Exception {
        Long memberId = 1L;

        try (MockedConstruction<SseEmitter> mockedConstruction = Mockito.mockConstruction(
            SseEmitter.class,
            (mock, context) -> doThrow(new IOException("Test IO error"))
                .when(mock).send(any(SseEmitter.SseEventBuilder.class))
        )) {
            SseException exception = assertThrows(SseException.class,
                () -> sseNotificationService.subscribe(memberId));

            assertThat(exception.getErrorCode()).isEqualTo(SSE_NOT_CONNECTED);
            assertThat(sseEmitters).doesNotContainKey(memberId);
            assertThat(mockedConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("subscribe: 기존 연결이 있으면 complete 후 새 emitter로 교체한다")
    void subscribe_replacesOldEmitter() throws Exception {
        Long memberId = 9L;

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
            SseEmitter.class,
            (mock, ctx) -> { /* 기본: send 성공 */ }
        )) {
            SseEmitter first = sseNotificationService.subscribe(memberId);
            SseEmitter second = sseNotificationService.subscribe(memberId);

            assertThat(sseEmitters).hasSize(1).containsEntry(memberId, second);

            SseEmitter firstMock = mocked.constructed().get(0);
            verify(firstMock, atLeastOnce()).complete();
        }
    }

    @Test
    @DisplayName("sendNotification: 정상 전송 시 예외 없이 send 호출된다")
    void sendNotification_success() throws Exception {
        Long memberId = 2L;

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
            SseEmitter.class,
            (mock, ctx) -> { /* send 성공 */ }
        )) {
            sseNotificationService.subscribe(memberId);

            assertDoesNotThrow(() ->
                sseNotificationService.sendNotification(
                    memberId,
                    com.grow.notification_service.notification.infra.persistence.entity.NotificationType.MATCHING_SUCCESS,
                    "msg"));

            SseEmitter mock = mocked.constructed().get(0);
            verify(mock, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Test
    @DisplayName("sendNotification: emitter가 없으면 SseException")
    void sendNotification_noEmitter_throws() {
        Long memberId = 999L;

        assertThrows(SseException.class, () ->
            sseNotificationService.sendNotification(
                memberId,
                com.grow.notification_service.notification.infra.persistence.entity.NotificationType.MATCHING_SUCCESS,
                "msg"));
    }

    @Test
    @DisplayName("sendHeartbeat: 성공 emitter 유지, 실패 emitter 제거")
    void sendHeartbeat_successAndRemoveOnFailure() throws Exception {
        Long okId = 100L;
        Long failId = 101L;

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
            SseEmitter.class,
            (mock, ctx) -> { /* connect send 성공 */ }
        )) {
            sseNotificationService.subscribe(okId);
            sseNotificationService.subscribe(failId);

            SseEmitter okEmitter   = mocked.constructed().get(0);
            SseEmitter failEmitter = mocked.constructed().get(1);

            doNothing().when(okEmitter).send(any(SseEmitter.SseEventBuilder.class));
            doThrow(new IOException("hb fail")).when(failEmitter).send(any(SseEmitter.SseEventBuilder.class));

            sseNotificationService.sendHeartbeat();

            @SuppressWarnings("unchecked")
            Map<Long, SseEmitter> map =
                (Map<Long, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "sseEmitters");

            assertThat(map).containsKey(okId);
            assertThat(map).doesNotContainKey(failId);
        }
    }

    @Test
    @DisplayName("handleNotificationSavedEvent: 이벤트 DTO를 그대로 sendNotification에 위임")
    void handleEvent_delegatesToSendNotification() {
        SseSendServiceImpl spySvc = Mockito.spy(sseNotificationService);

        var dto = com.grow.notification_service.notification.presentation.dto.NotificationRequestDto
            .builder()
            .memberId(77L)
            .notificationType(
                com.grow.notification_service.notification.infra.persistence.entity.NotificationType.MATCHING_SUCCESS)
            .content("hi")
            .build();
        var event = new NotificationSavedEvent(this, dto);

        doThrow(new SseException(SSE_NOT_CONNECTED)).when(spySvc)
            .sendNotification(eq(77L),
                eq(com.grow.notification_service.notification.infra.persistence.entity.NotificationType.MATCHING_SUCCESS),
                eq("hi"));

        assertThrows(SseException.class, () -> spySvc.handleNotificationSavedEvent(event));

        verify(spySvc, times(1)).sendNotification(77L,
            com.grow.notification_service.notification.infra.persistence.entity.NotificationType.MATCHING_SUCCESS,
            "hi");
    }
}