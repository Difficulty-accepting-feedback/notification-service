package com.grow.notification_service.analysis.application.event;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.grow.notification_service.global.util.JsonUtils;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisNotificationProducer {

	private static final String TOPIC = "analysis.notification.requested";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final Clock clock;

	public void analysisCompleted(Long memberId) {
		String content = "학습 분석 리포트가 도착했어요.";
		LocalDateTime now = LocalDateTime.now(clock);

		AnalysisNotificationEvent event = new AnalysisNotificationEvent(
			memberId,
			NotificationType.ANALYSIS,
			content,
			now
		);

		String key = String.valueOf(memberId);
		String payload = JsonUtils.toJsonString(event);

		kafkaTemplate.send(TOPIC, key, payload);
		log.info("[KAFKA][SENT] topic={}, key={}, type={}, content='{}'",
			TOPIC, key, NotificationType.ANALYSIS, content);
	}

	public void focusGuideReady(Long memberId) {
		String content = "오답 기반 학습 가이드가 준비됐어요.";
		LocalDateTime now = LocalDateTime.now(clock);

		AnalysisNotificationEvent event = new AnalysisNotificationEvent(
			memberId,
			NotificationType.ANALYSIS,
			content,
			now
		);

		String key = String.valueOf(memberId);
		String payload = JsonUtils.toJsonString(event);

		kafkaTemplate.send(TOPIC, key, payload);
		log.info("[KAFKA][SENT] topic={}, key={}, type={}, content='{}'",
			TOPIC, key, NotificationType.ANALYSIS, content);
	}
}