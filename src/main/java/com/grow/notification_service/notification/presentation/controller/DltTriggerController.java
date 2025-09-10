package com.grow.notification_service.notification.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/test/dlt")
public class DltTriggerController {

	private final KafkaTemplate<String, String> kafkaTemplate;

	/**
	 * 슬랙 트리거 테스트용 컨트롤러
	 * 형식이 깨진 json을 보내 DLT로 보내는 용두
	 * 실제 리트라이 로직 점검용
	 * 예: POST /internal/test/dlt/member
	 *     POST /internal/test/dlt/point
	 *     POST /internal/test/dlt/payment
	 *     POST /internal/test/dlt/qna
	 *     POST /internal/test/dlt/note
	 */
	@PostMapping("/{domain}")
	public String trigger(@PathVariable String domain,
		@RequestParam(defaultValue = "1") String key) {
		String topic = switch (domain.toLowerCase()) {
			case "member" -> "member.notification.requested";
			case "point"  -> "point.notification.requested";
			case "payment"-> "payment.notification.requested";
			case "qna"    -> "qna.notification.requested";
			case "note"   -> "note.notification.requested";
			default       -> throw new IllegalArgumentException("unknown domain: " + domain);
		};

		String brokenPayload = "{ this_is: not_valid_json, because: [unclosed }";

		kafkaTemplate.send(topic, key, brokenPayload);
		log.info("[TEST][DLT] topic={}, key={}", topic, key);
		return "SENT to " + topic;
	}
}