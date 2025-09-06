package com.grow.notification_service.notification.infra.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.global.config.JsonUtils;
import com.grow.notification_service.notification.application.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.*;

import java.util.Map;

class NotificationRequestedConsumerTest {

	private NotificationService notificationService; // mock
	private JsonUtils json;                          // real
	private NotificationRequestedConsumer consumer;  // SUT

	@BeforeEach
	void setUp() {
		notificationService = mock(NotificationService.class);
		consumer = new NotificationRequestedConsumer(notificationService, json);
	}

	@Test
	void onMessage_success_withObjectMapper() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
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
	@DisplayName("깨진 JSON이면 컨슈머가 예외를 잡고 Service는 호출하지 않음")
	void onMessage_badJson() {
		// given: 명백히 잘못된 JSON
		String badPayload = "{ not-a-json ";

		// when: 컨슈머가 try-catch로 예외를 먹고 로그만 남겨야 함
		consumer.onMessage(badPayload);

		// then: 서비스 호출 안 됨
		verify(notificationService, never()).processNotification(any());
	}
}