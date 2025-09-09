package com.grow.notification_service.notification.infra.kafka;

import com.grow.notification_service.global.slack.SlackErrorSendService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 알림 요청 이벤트가 재시도 끝에 실패(DLT 도착)했을 때 처리하는 Consumer
 * - 로그/모니터링 동일 규격 유지
 * - 슬랙 웹훅 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestedEventDltConsumer {

	private final SlackErrorSendService slackErrorSendService;

	@KafkaListener(
		topics = "member.notification.requested.dlt",
		groupId = "notification-dlt-member-service"
	)
	public void consumeMemberDlt(String message) {
		log.info("[NOTIFICATION DLT][MEMBER] 수신: {}", message == null ? "" : message.trim());

		slackErrorSendService.sendError(
			"회원 알림 - 전송 실패",
			"카테고리: [MEMBER -> NOTIFICATION]\n상세: 회원 관련 알림 전송에 실패하였습니다.\n영향: 사용자에게 알림이 수신되지 않아 안내 지연 가능",
			message
		);

		log.info("[NOTIFICATION DLT][MEMBER] 처리 완료");
	}

	@KafkaListener(
		topics = "point.notification.requested.dlt",
		groupId = "notification-dlt-point-service"
	)
	public void consumePointDlt(String message) {
		log.info("[NOTIFICATION DLT][POINT] 수신: {}", message == null ? "" : message.trim());

		slackErrorSendService.sendError(
			"포인트 알림 - 전송 실패",
			"카테고리: [POINT -> NOTIFICATION]\n상세: 포인트 관련 알림 전송에 실패하였습니다.\n영향: 사용자에게 알림이 수신되지 않아 안내 지연 가능",
			message
		);

		log.info("[NOTIFICATION DLT][POINT] 처리 완료");
	}

	@KafkaListener(
		topics = "payment.notification.requested.dlt",
		groupId = "notification-dlt-payment-service"
	)
	public void consumePaymentDlt(String message) {
		log.info("[NOTIFICATION DLT][PAYMENT] 수신: {}", message == null ? "" : message.trim());

		slackErrorSendService.sendError(
			"결제 알림 - 전송 실패",
			"카테고리: [PAYMENT -> NOTIFICATION]\n상세: 결제 관련 알림 전송에 실패하였습니다.\n영향: 사용자에게 알림이 수신되지 않아 안내 지연 가능",
			message
		);

		log.info("[NOTIFICATION DLT][PAYMENT] 처리 완료");
	}

	@KafkaListener(
		topics = "qna.notification.requested.dlt",
		groupId = "notification-dlt-qna-service"
	)
	public void consumeQnaDlt(String message) {
		log.info("[NOTIFICATION DLT][QNA] 수신: {}", message == null ? "" : message.trim());
		slackErrorSendService.sendError(
			"QnA 알림 - 전송 실패",
			"카테고리: [QNA -> NOTIFICATION]\n상세: QnA 관련 알림 전송에 실패하였습니다.\n영향: 사용자에게 알림이 수신되지 않아 안내 지연 가능",
			message
		);
		log.info("[NOTIFICATION DLT][QNA] 처리 완료");
	}
}