package com.grow.notification_service.global.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class NotificationMetrics {

	private final MeterRegistry registry;

	/** 상태 전이 카운터(필요 시 사용) */
	public void transition(String from, String to) {
		registry.counter("notification_state_transition_total", "from", v(from), "to", v(to)).increment();
	}

	/** 결과 카운터: 이름과 태그만 주면 카운트 */
	public void result(String name, String... tags) {
		registry.counter(name, tags).increment();
	}

	/** null/blank 라벨 방지 */
	public static String v(String s) {
		return (s == null || s.isBlank()) ? "unknown" : s;
	}
}