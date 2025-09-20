package com.grow.notification_service.analysis.application.event;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.grow.notification_service.global.util.JsonUtils;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizNotificationProducer {

	private static final String TOPIC = "quiz.notification.requested";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final Clock clock;

	public void quizGenerated(Long memberId, Long categoryId, List<Long> quizIds, boolean fromWrong) {
		String content = fromWrong
			? "오답 기반 복습 문제가 생성됐어요."
			: "새 퀴즈 세트가 준비됐어요.";

		QuizNotificationEvent event = new QuizNotificationEvent(
			memberId,
			NotificationType.QUIZ,
			content,
			LocalDateTime.now(clock)
		);

		String key = String.valueOf(memberId);
		String payload = JsonUtils.toJsonString(event);

		kafkaTemplate.send(TOPIC, key, payload);
		log.info("[KAFKA][SENT] topic={}, key={}, type={}, quizCnt={}",
			TOPIC, key, NotificationType.QUIZ, (quizIds == null ? 0 : quizIds.size()));
	}
}