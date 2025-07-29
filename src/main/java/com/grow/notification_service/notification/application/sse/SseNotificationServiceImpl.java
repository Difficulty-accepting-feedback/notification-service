package com.grow.notification_service.notification.application.sse;

import com.grow.notification_service.notification.application.exception.SseException;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.grow.notification_service.notification.application.exception.ErrorCode.SSE_NOT_CONNECTED;

/**
 * <h2> SSE (Server-Sent Events)를 이용한 알림 서비스의 구현 클래스 </h2>
 * 사용자 ID별로 SSE Emitter를 관리하며, 클라이언트의 구독 요청을 처리하고
 * 실시간 알림을 전송하는 역할을 담당합니다.
 *
 * <p>이 클래스는 ConcurrentHashMap을 사용하여 멀티스레드 환경에서 안전하게
 * Emitter를 저장합니다. 연결이 많아질 경우 서버 리소스 소비를 고려하여
 * Redis Pub/Sub 구조로 확장하는 것을 고려합니다.
 *
 * <p><b>주요 기능:</b>
 * <ul>
 *     <li>사용자 ID 기반 SSE 연결 구독</li>
 *     <li>초기 연결 확인을 위한 더미 이벤트 전송</li>
 *     <li>알림 메시지 전송</li>
 * </ul>
 *
 * <p><b>주의:</b> Emitter의 타임아웃은 기본적으로 1시간으로 설정되어 있으며,
 * 필요에 따라 조정할 수 있습니다. 연결 실패 시 커스텀 예외(SseException)를 발생시킵니다.
 * @see SseEmitter
 * @see SseException
 * @since 25.07.29 - 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseNotificationServiceImpl implements SseNotificationService {

    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /**
     * 클라이언트가 SSE 연결을 구독할 때 호출되는 메서드입니다.
     * 주어진 memberId에 해당하는 SseEmitter를 생성하고 Map에 저장한 후,
     * 초기 연결 확인을 위한 더미 이벤트를 전송합니다.
     *
     * <p>타임아웃은 1시간(60분)으로 설정되어 있으며, 연결 성공 시 "[connect]" 이름의
     * 이벤트와 "연결이 성공했습니다!" 메시지를 전송합니다. 실패 시 SseException을 발생시킵니다.
     *
     * <p><b>로그:</b> 연결 성공 시 INFO 레벨 로그를 기록하며, 실패 시 ERROR 레벨 로그를 기록합니다.
     *
     * @param memberId 구독하는 사용자의 ID. Long 타입으로, null이 아닌 유효한 값이어야 합니다.
     * @return 생성된 SseEmitter 객체. 이를 통해 SSE 연결이 유지됩니다.
     * @throws SseException 연결 중 IOException 발생 시 예외를 감싸서 던집니다.
     */
    @Override
    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60); // 타임아웃 1시간 (더 늘려야 할 수도 있음... 몰라서 일단 한 시간 함)

        // 연결이 되었을 시에 더미 이벤트 전송 (연결 유지 테스트)
        try {
            emitter.send(SseEmitter.event().name("[connect]").data("연결이 성공했습니다!"));
            sseEmitters.put(memberId, emitter);
            log.info("[Matching Notification] SSE 연결 성공 - memberId: {}", memberId);
        } catch (IOException e) {
            log.error("[Matching Notification] SSE 연결 실패 - memberId: {}", memberId);
            throw new SseException(SSE_NOT_CONNECTED, e); // 예외 감싸서 전파
        }

        return emitter;
    }

    /**
     * 알림 메시지를 전송하는 메서드입니다.
     * (현재 구현되지 않음 - 실제 로직 추가 필요)
     *
     * @param message 전송할 알림 메시지.
     */
    @Override
    public void sendNotification(String message) {

    }
}
