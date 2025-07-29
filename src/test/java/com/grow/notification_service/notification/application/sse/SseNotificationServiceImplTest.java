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

import static com.grow.notification_service.notification.application.exception.ErrorCode.SSE_NOT_CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class SseNotificationServiceImplTest {

    @InjectMocks
    private SseNotificationServiceImpl sseNotificationService;

    private Map<Long, SseEmitter> sseEmitters;

    @BeforeEach
    void setUp() throws Exception {
        sseEmitters = new HashMap<>();
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
}