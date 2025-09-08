package com.grow.notification_service.notification.application.event.consumer.dlt;

import com.grow.notification_service.global.slack.SlackErrorSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupJoinRequestNotificationDltConsumer {

    private final SlackErrorSendService slackErrorSendService;

    @KafkaListener(
            topics = "group.join-request.notification.dlt",
            groupId = "join-request-dlt-service"
    )
    public void consumeDlt(String message) {
        log.info("[GROUP JOIN REQUEST DLT] 알림 요청 실패 이벤트 수신: {}", message == null ? "" : message.trim());

        // TODO 로그 시스템에 전송 or 모니터링 카운트 증가

        // 슬랙으로 알림 전송
        slackErrorSendService.sendError("그룹 가입 - 알림 전송 실패",
                "카테고리: [GROUP JOIN REQUEST -> NOTIFICATION]\n상세: 그룹 가입(요청, 수락, 거절 등) 알림 전송에 실패하였습니다.\n영향: 사용자에게 알림이 수신되지 않아, 확인 지연 가능 및 불편함 증가",
                message);

        log.info("[GROUP JOIN REQUEST DLT] 알림 요청 실패 이벤트 처리 완료");
    }
}
