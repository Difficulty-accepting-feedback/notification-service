package com.grow.notification_service.qna.application.event;

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
public class QnaNotificationProducer {

	private static final String TYPE = "QNA";
	private static final String CODE_ANSWER_ADDED = "ANSWER_ADDED";
	private static final String TOPIC = "qna.notification.requested";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final Clock clock;

	public void answerAdded(Long recipientMemberId, Long answerId) {
		String content = "내 질문에 새로운 답변이 달렸어요.";
		LocalDateTime now = LocalDateTime.now(clock);

		QnaNotificationEvent event = new QnaNotificationEvent(
			recipientMemberId,
			CODE_ANSWER_ADDED,
			TYPE,
			null,
			content,
			answerId,
			now
		);

		String key = String.valueOf(recipientMemberId);
		String payload = JsonUtils.toJsonString(event);

		kafkaTemplate.send(TOPIC, key, payload);
		log.info("[KAFKA][SENT] topic={}, key={}, code={}, answerId={}",
			TOPIC, key, CODE_ANSWER_ADDED, answerId);
	}
}