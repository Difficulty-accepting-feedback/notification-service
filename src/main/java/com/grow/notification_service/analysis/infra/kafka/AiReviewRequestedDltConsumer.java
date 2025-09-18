package com.grow.notification_service.analysis.infra.kafka;

import com.grow.notification_service.global.slack.SlackErrorSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * AI 리뷰 요청 이벤트가 재시도 끝에 실패(DLT 도착)했을 때 처리하는 Consumer
 * - 로그 및 슬랙 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReviewRequestedDltConsumer {

	private final SlackErrorSendService slackErrorSendService;

	@KafkaListener(
		topics = "member.ai-review.requested.dlt",
		groupId = "ai-review-dlt"
	)
	public void consumeDlt(
		String message,
		@Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
		@Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
		@Header(value = KafkaHeaders.RECEIVED_TIMESTAMP, required = false) Long timestamp
	) {
		String safeMsg = message == null ? "" : message.trim();
		log.error("[AI-REVIEW DLT] topic={} partition={} offset={} ts={} payload={}",
			topic, offset, timestamp, safeMsg);

		slackErrorSendService.sendError(
			"AI 복습 퀴즈 생성 - 처리 실패",
			"카테고리: [AI-REVIEW]\n"
				+ "상세: AI 복습 퀴즈 생성 요청이 재시도 끝에 실패하여 DLT로 이동했습니다.\n"
				+ "메타: topic=%s, offset=%s, timestamp=%s"
				.formatted(topic, String.valueOf(offset), String.valueOf(timestamp)),
			safeMsg
		);

		log.info("[AI-REVIEW DLT] 처리 완료");
	}
}