package com.grow.notification_service.notification.infra.kafka;

import com.slack.api.Slack;
import com.slack.api.webhook.WebhookPayloads;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;

/**
 * 알림 요청 이벤트가 재시도 끝에 실패(DLT 도착)했을 때 처리하는 Consumer
 * - 로그/모니터링 동일 규격 유지
 * - 슬랙 웹훅 알림 전송
 */
@Slf4j
@Service
public class NotificationRequestedEventDltConsumer {

	@Value("${slack.webhook.url}")
	private String slackWebhookUrl;

	@KafkaListener(
		topics = "member.notification.requested.dlt",
		groupId = "notification-dlt-service"
	)
	public void consumeNotificationRequestedDlt(String message) {
		log.info("[NOTIFICATION DLT] 알림 요청 실패 이벤트 수신: {}", message == null ? "" : message.trim());

		// TODO 로그 시스템에 전송 or 모니터링 카운트 증가 + 슬랙 URL 받아서 테스트 해봐야함

		// 슬랙으로 알림 전송
		Slack slack = Slack.getInstance();

		// 현재 시간(KST) 동적 생성
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
		String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		// 에러 메시지 구성
		String errorDetails =
			"카테고리: [NOTIFICATION]\n" +
				"상세: 알림 처리에 실패하였습니다.\n" +
				"발생 시간: " + currentTime + "\n" +
				"영향: 사용자에게 알림 전송이 누락되어 안내 지연 가능";

		try {
			slack.send(slackWebhookUrl, WebhookPayloads.payload(p -> p.blocks(asBlocks(
				header(h -> h.text(plainText("⚠️ 오류 알림: 알림 요청 이벤트 수신 실패", true))),
				section(s -> s.text(plainText(errorDetails)))
			))));
		} catch (IOException e) {
			log.warn("[NOTIFICATION DLT] 알림 요청 실패 이벤트 수신 실패: {}", e.getMessage());
			throw new RuntimeException("slack 에 오류 메시지 전송 실패", e);
		}
	}
}