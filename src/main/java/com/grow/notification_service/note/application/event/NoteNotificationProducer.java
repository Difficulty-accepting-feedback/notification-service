package com.grow.notification_service.note.application.event;

import java.time.LocalDateTime;
import java.time.Clock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.grow.notification_service.global.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteNotificationProducer {

	private static final String TYPE = "NOTE";
	private static final String CODE_NOTE_RECEIVED = "NOTE_RECEIVED";
	private static final String TOPIC = "note.notification.requested";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final Clock clock;

	public void noteReceived(Long recipientMemberId, Long noteId) {
		String content = "새 쪽지가 도착했어요.";
		LocalDateTime now = LocalDateTime.now(clock);

		NoteNotificationEvent event = new NoteNotificationEvent(
			recipientMemberId,
			CODE_NOTE_RECEIVED,
			TYPE,
			null,
			content,
			noteId,
			now
		);

		String key = String.valueOf(recipientMemberId); // 쪽지 순서 보장을 위해 key 설정
		String payload = JsonUtils.toJsonString(event);

		kafkaTemplate.send(TOPIC, key, payload);
		log.info("[KAFKA][SENT] topic={}, key={}, code={}, noteId={}",
			TOPIC, key, CODE_NOTE_RECEIVED, noteId);
	}
}