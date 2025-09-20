package com.grow.notification_service.notification.infra.kafka;

import com.grow.notification_service.global.util.JsonUtils;
import com.grow.notification_service.notification.application.service.NotificationService;
import com.grow.notification_service.notification.presentation.dto.NotificationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestedConsumer {

	private final NotificationService notificationService;

	/**
	 * Kafka "member.notification.requested" 토픽에서 메시지를 수신합니다.
	 * 메시지 페이로드를 NotificationRequestDto로 역직렬화하고,
	 * NotificationService를 통해 알림 처리를 수행합니다.
	 * 수신된 메시지와 처리 결과를 로그에 기록합니다.
	 * 예외 발생 시 에러 로그를 남기고, 필요 시 DLQ 재전송 로직을 추가할 수 있습니다.
	 * @param payload Kafka 메시지의 JSON 페이로드
 	 */
	@KafkaListener(
		topics = {
			"member.notification.requested",
			"point.notification.requested",
			"payment.notification.requested",
			"qna.notification.requested",
			"note.notification.requested",
			"quiz.notification.requested",
			"analysis.notification.requested"
		},
		groupId = "notification-service",
		concurrency = "3"
	)
	@RetryableTopic(
		attempts = "5",
		backoff = @Backoff(delay = 1000, multiplier = 2),
		dltTopicSuffix = ".dlt",
		autoCreateTopics = "true"
	)
	public void onMessage(String payload) {
		// 1. 페이로드를 NotificationRequestDto로 역직렬화
		// 2. NotificationService를 통해 알림 처리
		// 3. 처리 결과를 로그에 기록
		try {
			NotificationRequestDto dto = JsonUtils.fromJsonString(payload, NotificationRequestDto.class);
			notificationService.processNotification(dto);
		} catch (Exception e) {
			log.error("[KAFKA][RECV][ERROR] payload={}", payload, e);
			throw e;
		}
	}
}