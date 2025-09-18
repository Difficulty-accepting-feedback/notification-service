package com.grow.notification_service.quiz.application.event;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.grow.notification_service.global.util.JsonUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 리뷰 요청 이벤트 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReviewRequestedProducer {
	private static final String TOPIC = "member.ai-review.requested";
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final Clock clock;

	/**
	 * AI 리뷰 요청 이벤트 발행
	 * @param memberId 멤버 ID
	 * @param categoryId 카테고리 ID
	 * @param levelParam 레벨 파라미터
	 * @param topic 주제
	 * @param dedupeKey 중복 방지 키
	 */
	public void publish(Long memberId, Long categoryId, String levelParam, String topic, String dedupeKey) {
		LocalDateTime now = LocalDateTime.now(clock);
		AiReviewRequestedEvent event = new AiReviewRequestedEvent(
			memberId, categoryId, levelParam, topic, dedupeKey, now
		);
		String key = String.valueOf(memberId);
		kafkaTemplate.send(TOPIC, key, JsonUtils.toJsonString(event));
		log.info("[KAFKA][SENT] topic={} key={} memberId={} categoryId={} dedupeKey={}",
			TOPIC, key, memberId, categoryId, dedupeKey);
	}
}