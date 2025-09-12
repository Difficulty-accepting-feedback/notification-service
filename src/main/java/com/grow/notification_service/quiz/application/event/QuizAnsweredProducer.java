package com.grow.notification_service.quiz.application.event;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.grow.notification_service.global.util.JsonUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizAnsweredProducer {

	private static final String TOPIC = "member.quiz.answered";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final Clock clock;

	/**
	 * 퀴즈 정답 제출 이벤트 발행
	 */
	public void publish(Long memberId,
		Long quizId,
		Long categoryId,
		String level,
		String submitted,
		boolean correct) {

		LocalDateTime now = LocalDateTime.now(clock);

		QuizAnsweredEvent event = new QuizAnsweredEvent(
			memberId,
			quizId,
			categoryId,
			level,
			submitted,
			correct,
			now
		);

		String key = String.valueOf(memberId); // 멤버별 순서 보장
		String payload = JsonUtils.toJsonString(event);

		kafkaTemplate.send(TOPIC, key, payload);
		log.info("[KAFKA][SENT] topic={} key={} memberId={} quizId={} correct={}",
			TOPIC, key, memberId, quizId, correct);
	}
}