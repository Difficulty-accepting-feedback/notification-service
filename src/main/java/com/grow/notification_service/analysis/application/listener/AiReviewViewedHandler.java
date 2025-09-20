package com.grow.notification_service.analysis.application.listener;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import com.grow.notification_service.analysis.application.event.AiReviewViewedEvent;
import com.grow.notification_service.analysis.application.service.AnalysisApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiReviewViewedHandler {

	private final AnalysisApplicationService analysisApplicationService;
	private final StringRedisTemplate redis;

	private static final Duration DEDUPE_TTL = Duration.ofMinutes(3);

	@Async("aiReviewExecutor")
	@EventListener
	public void handle(AiReviewViewedEvent evt) {
		Long memberId = evt.memberId();
		Long categoryId = evt.categoryId();
		String sessionId = evt.sessionId();
		List<Long> quizIds = evt.quizIds();

		String hash = DigestUtils.md5DigestAsHex(
			(memberId + ":" + categoryId + ":" + sessionId + ":" + quizIds.toString()).getBytes(StandardCharsets.UTF_8)
		);
		String key = "ai-review:latest:analyze:" + hash;

		try {
			Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", DEDUPE_TTL);
			if (Boolean.FALSE.equals(acquired)) {
				log.info("[AI-REVIEW][ASYNC] 멱등 실패 - {}", key);
				return;
			}
			// 실제 분석 수행
			analysisApplicationService.analyzeFromQuizIds(memberId, categoryId, sessionId, quizIds);
			log.info("[AI-REVIEW][ASYNC] 분석 - mid={}, cid={}, sid={}, size={}",
				memberId, categoryId, sessionId, quizIds.size());
		} catch (Exception e) {
			log.warn("[AI-REVIEW][ASYNC] 실패 - mid={}, cid={}, sid={}, err={}",
				memberId, categoryId, sessionId, e.toString(), e);
		}
	}
}