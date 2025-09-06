package com.grow.notification_service.notification.infra.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.grow.notification_service.notification.application.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationRequestedConsumerTest {

	private NotificationService notificationService; // mock
	private NotificationRequestedConsumer consumer;  // SUT

	@BeforeEach
	void setUp() {
		notificationService = mock(NotificationService.class);
		// 컨슈머는 NotificationService만 주입받음 (@RequiredArgsConstructor)
		consumer = new NotificationRequestedConsumer(notificationService);
	}

	@Test
	@DisplayName("정상 JSON이면 역직렬화 후 도메인 서비스 호출")
	void onMessage_success() throws Exception {
		// JsonUtils가 내부에서 JavaTimeModule을 등록해 쓰므로,
		// 테스트에서도 같은 포맷으로 JSON을 만들어 주는 게 안전해요.
		ObjectMapper mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		String payload = mapper.writeValueAsString(
			Map.of(
				"memberId", 14,
				"code", "ADDR_REMINDER",
				"notificationType", "SERVICE_NOTICE",
				"title", "주소 정보 미입력",
				"content", "더 정확한 매칭을 위해 주소 정보를 등록해 주세요.",
				"occurredAt", "2025-09-06T18:29:46.4550951"
			)
		);

		consumer.onMessage(payload);

		verify(notificationService, times(1)).processNotification(any());
	}

	@Test
	@DisplayName("깨진 JSON이면 예외를 잡고 서비스는 호출하지 않음")
	void onMessage_badJson() {
		// given
		String payload = "{ not-a-json";

		// when & then
		assertThrows(RuntimeException.class, () -> consumer.onMessage(payload));
		verify(notificationService, never()).processNotification(any());
	}
}