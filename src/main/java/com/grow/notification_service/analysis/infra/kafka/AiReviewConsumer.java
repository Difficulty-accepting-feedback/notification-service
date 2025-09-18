package com.grow.notification_service.analysis.infra.kafka;

import java.time.Duration;
import java.util.List;

import com.grow.notification_service.analysis.infra.persistence.entity.MemberLatestAiReviewJpaEntity;
import com.grow.notification_service.analysis.infra.persistence.repository.MemberLatestAiReviewJpaRepository;
import com.grow.notification_service.global.util.JsonUtils;
import com.grow.notification_service.analysis.application.service.QuizGenerationApplicationService;
import com.grow.notification_service.quiz.application.event.AiReviewRequestedEvent;
import com.grow.notification_service.quiz.application.port.SubscriptionPort;
import com.grow.notification_service.quiz.domain.model.Quiz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiReviewConsumer {

	private final SubscriptionPort subscriptionPort;
	private final QuizGenerationApplicationService quizGen;
	private final StringRedisTemplate redis;
	private final MemberLatestAiReviewJpaRepository latestRepo;

	private static final Duration DEDUPE_TTL = Duration.ofMinutes(10); // 10분 내 중복 요청 차단

	/**
	 * AI 복습 퀴즈 생성 요청 수신
	 * @param payload JSON 페이로드
	 */
	@KafkaListener(
		topics = "member.ai-review.requested",
		groupId = "ai-review",
		concurrency = "3"
	)
	@RetryableTopic(
		attempts = "${kafka.retry.worker.attempts:5}",
		backoff = @Backoff(delay = 1000, multiplier = 2),
		dltTopicSuffix = ".dlt",
		autoCreateTopics = "true"
	)
	public void onMessage(@Payload String payload) {
		try {
			AiReviewRequestedEvent evt = JsonUtils.fromJsonString(payload, AiReviewRequestedEvent.class);

			// 구독 확인
			if (!subscriptionPort.canGenerateAiReview(evt.memberId())) {
				log.info("[AI-REVIEW][BLOCKED] 구독 비활성 - memberId={}, categoryId={}", evt.memberId(), evt.categoryId());
				return;
			}

			// 중복 방지
			String key = (evt.dedupeKey() != null && !evt.dedupeKey().isBlank())
				? evt.dedupeKey()
				: "ai-review:req:%d:%d".formatted(evt.memberId(), evt.categoryId());

			// 키가 이미 존재하면 중복으로 간주하고 스킵
			if (!tryAcquireDedupe(key, DEDUPE_TTL)) {
				log.info("[AI-REVIEW][SKIP] 중복 스킵 - {}", key);
				return;
			}

			// 복습 퀴즈 생성
			List<Quiz> saved = quizGen.generateQuizzesFromWrong(
				evt.memberId(), evt.categoryId(), evt.levelParam(), evt.topic()
			);
			log.info("[AI-REVIEW][GEN][END] memberId={}, categoryId={}, saved={}",
				evt.memberId(), evt.categoryId(), saved.size());

			// 방금 생성된 퀴즈 저장
			List<Long> ids = saved.stream()
				.map(Quiz::getQuizId)
				.toList();

			MemberLatestAiReviewJpaEntity row = MemberLatestAiReviewJpaEntity.builder()
				.memberId(evt.memberId())
				.categoryId(evt.categoryId())
				.quizIdsJson(JsonUtils.toJsonString(ids))
				.updatedAt(java.time.LocalDateTime.now())
				.build();

			latestRepo.save(row);
			log.info("[AI-REVIEW][QUIZ][SAVE} AI 생성 퀴즈 저장 완료");

		} catch (Exception e) {
			log.error("[KAFKA][AI-REVIEW-WORKER][ERROR] payload={}", payload, e);
			throw e;
		}
	}

	// 헬퍼

	/**
	 * 중복 요청 방지용
	 * @param key 중복 방지 키
	 * @param ttl 만료 시간
	 * @return 획득 성공 여부
	 */
	private boolean tryAcquireDedupe(String key, Duration ttl) {
		try {
			Boolean ok = redis.opsForValue().setIfAbsent(key, "1", ttl);
			return Boolean.TRUE.equals(ok);
		} catch (Exception e) {
			log.warn("[AI-REVIEW][DEDUPE][FAIL] key={}", key, e);
			return false;
		}
	}
}