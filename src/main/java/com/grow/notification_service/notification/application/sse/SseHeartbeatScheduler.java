package com.grow.notification_service.notification.application.sse;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {
	private final SseSendService sseSendService;

	@Scheduled(fixedDelay = 25_000) //25초
	public void heartbeat() {
		sseSendService.sendHeartbeat();
	}
}