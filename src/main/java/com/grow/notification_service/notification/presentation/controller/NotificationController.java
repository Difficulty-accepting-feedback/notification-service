package com.grow.notification_service.notification.presentation.controller;

import com.grow.notification_service.notification.application.NotificationService;
import com.grow.notification_service.notification.application.sse.SseNotificationService;
import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import com.grow.notification_service.notification.presentation.dto.rsdata.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    /**
     * SSE (Server-Sent Events) 연결을 위한 구독 엔드포인트입니다.
     * 이 메서드는 클라이언트가 최초로 SSE 연결을 시도할 때 호출되며,
     * API Gateway에서 전달된 헤더를 통해 memberId를 추출하여 서비스에 전달합니다.
     * 연결이 성공적으로 이루어지면 SseEmitter 객체를 반환하여 지속적인 이벤트 스트림을 제공합니다.
     *
     * <p>이 엔드포인트는 GET 요청을 처리하며, text/event-stream 형식으로 응답을 생성합니다.
     * 클라이언트는 이 엔드포인트를 통해 서버로부터 실시간 알림을 수신할 수 있습니다.
     *
     * <p><b>주의:</b> memberId는 "X-Authorization-Id" 헤더를 통해 전달되어야 하며,
     * 이 값은 API Gateway에서 인증된 사용자 ID로 가정합니다. 연결 후 이벤트 전송은
     * 별도의 서비스 로직에서 처리됩니다.
     *
     * @param memberId 클라이언트의 사용자 ID (헤더에서 추출됨). Long 타입으로, null이 아닌 유효한 ID여야 합니다.
     * @return SseEmitter 객체. 이 객체를 통해 서버-클라이언트 간 SSE 연결이 유지됩니다.
     */
    @GetMapping(value = "/api/v1/notification/subscribe",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribe(@RequestHeader("X-Authorization-Id") Long memberId) {
        return sseNotificationService.subscribe(memberId);
    }
}