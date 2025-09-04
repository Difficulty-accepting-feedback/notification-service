package com.grow.notification_service.notification.application.sse;

import com.grow.notification_service.notification.application.exception.SseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.grow.notification_service.notification.application.exception.ErrorCode.SSE_NOT_CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseSendServiceImplTest {

    @InjectMocks
    private SseSendServiceImpl sseNotificationService;

    private Map<Long, SseEmitter> sseEmitters;

    @BeforeEach
    void setUp() throws Exception {
        sseEmitters = new ConcurrentHashMap<>();
        // 리플렉션을 사용해 private 필드 sseEmitters에 접근하고 값 주입
        ReflectionTestUtils.setField(sseNotificationService, "sseEmitters", sseEmitters);
    }

    @Test
    @DisplayName("SSE 연결 성공 테스트")
    void subscribe_success() throws Exception {
        // given
        Long memberId = 1L;

        // when
        SseEmitter result = sseNotificationService.subscribe(memberId);

        // then
        assertNotNull(result);
        assertTrue(sseEmitters.containsKey(memberId));
        assertEquals(result, sseEmitters.get(memberId));
        assertThat(sseEmitters.get(memberId)).isInstanceOf(SseEmitter.class); // map 저장 확인
    }

    @Test
    @DisplayName("SSE 연결 실패 테스트 - IOException 발생 시 SseException 던짐")
    void subscribe_failureWhenIOException() throws Exception {
        // given
        Long memberId = 1L;

        // SseEmitter 생성을 모킹하여 send 메서드가 IOException을 던지도록 설정
        try (MockedConstruction<SseEmitter> mockedConstruction = Mockito.mockConstruction(SseEmitter.class,
                (mock, context) -> doThrow(new IOException("Test IO error")).when(mock).send(any(SseEmitter.SseEventBuilder.class)))) {

            // when & then
            SseException exception = assertThrows(SseException.class, () -> sseNotificationService.subscribe(memberId));

            assertThat(exception.getErrorCode()).isEqualTo(SSE_NOT_CONNECTED);
            assertThat(sseEmitters).doesNotContainKey(memberId); // 실패 시 맵에 추가되지 않음
            assertThat(mockedConstruction.constructed()).hasSize(1); // SseEmitter가 한 번 생성되었는지 확인
        }
    }

    @Test
    @DisplayName("subscribe: 기존 연결이 있으면 complete 후 새 emitter로 교체한다")
    void subscribe_replacesOldEmitter() throws Exception {
        Long memberId = 9L;

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
            SseEmitter.class,
            (mock, ctx) -> { /* 첫번째/두번째 모두 기본 동작: send()는 성공 */ }
        )) {
            // first subscribe
            SseEmitter first = sseNotificationService.subscribe(memberId);
            // second subscribe (교체)
            SseEmitter second = sseNotificationService.subscribe(memberId);

            // 맵엔 두 번째만 남는다
            assertThat(sseEmitters).hasSize(1).containsEntry(memberId, second);

            // 첫 번째 complete() 호출되었는지 확인
            SseEmitter firstMock = mocked.constructed().get(0);
            verify(firstMock, atLeastOnce()).complete();
        }
    }

    @Test
    @DisplayName("sendNotification: 정상 전송 시 예외 없이 send 호출된다")
    void sendNotification_success() throws Exception {
        Long memberId = 2L;

        // subscribe로 emitter 등록
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

            // 실제 send 호출 확인(빌더 내용 검증은 복잡하니 any()로 충분)
            SseEmitter mock = mocked.constructed().get(0);
            verify(mock, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Test
    @DisplayName("sendNotification: emitter가 없으면 SseException")
    void sendNotification_noEmitter_throws() {
        Long memberId = 999L; // 맵에 없음
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
            (mock, ctx) -> { /* 기본은 doNothing (구독 connect용) */ }
        )) {
            // 1) 구독: 둘 다 connect send는 성공해야 함
            sseNotificationService.subscribe(okId);
            sseNotificationService.subscribe(failId);

            // constructed() 순서는 subscribe 호출 순서와 동일
            SseEmitter okEmitter   = mocked.constructed().get(0);
            SseEmitter failEmitter = mocked.constructed().get(1);

            // 2) okEmitter는 계속 성공
            doNothing().when(okEmitter).send(any(SseEmitter.SseEventBuilder.class));

            // 3) failEmitter는 "이 시점 이후 모든 send에서" IOException
            //    (구독은 이미 끝났으므로 하트비트 호출이 곧 실패)
            doThrow(new IOException("hb fail"))
                .when(failEmitter).send(any(SseEmitter.SseEventBuilder.class));

            // 4) heartbeat 실행
            sseNotificationService.sendHeartbeat();

            // 5) ok는 남아 있고, fail은 제거됨
            @SuppressWarnings("unchecked")
            Map<Long, SseEmitter> map =
                (Map<Long, SseEmitter>) ReflectionTestUtils.getField(sseNotificationService, "sseEmitters");

            assertThat(map).containsKey(okId);
            assertThat(map).doesNotContainKey(failId); // ✅ 여기서 통과
        }
    }
    @Test
    @DisplayName("handleNotificationSavedEvent: 이벤트 DTO를 그대로 sendNotification에 위임")
    void handleEvent_delegatesToSendNotification() {
        // partial spy로 sendNotification 호출만 검증
        SseSendServiceImpl spySvc = Mockito.spy(sseNotificationService);

        var dto = com.grow.notification_service.notification.presentation.dto.NotificationRequestDto
            .builder()
            .memberId(77L)
            .notificationType(
                com.grow.notification_service.notification.infra.persistence.entity.NotificationType.MATCHING_SUCCESS)
            .content("hi")
            .build();
        var event = new com.grow.notification_service.notification.application.event.NotificationSavedEvent(this, dto);

        // emitter가 없어도 sendNotification 내부에서 예외를 던질 테니, 호출 자체만 검증
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