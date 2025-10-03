package com.grow.notification_service.quiz.application.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QuizException;
import com.grow.notification_service.global.metrics.NotificationMetrics;
import com.grow.notification_service.quiz.application.event.AiReviewRequestedProducer;
import com.grow.notification_service.quiz.application.port.MemberQuizResultPort;
import com.grow.notification_service.quiz.application.dto.QuizItem;
import com.grow.notification_service.quiz.application.event.QuizAnsweredProducer;
import com.grow.notification_service.quiz.application.mapping.SkillTagToCategoryRegistry;
import com.grow.notification_service.quiz.application.service.QuizApplicationService;
import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;
import com.grow.notification_service.quiz.domain.service.QuizReviewService;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswerItem;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswerResult;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersRequest;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersResponse;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizApplicationServiceImpl implements QuizApplicationService {

	private final SkillTagToCategoryRegistry registry;
	private final MemberQuizResultPort memberResultPort;
	private final QuizRepository quizRepository;
	private final QuizAnsweredProducer eventPublisher;
	private final AiReviewRequestedProducer aiReviewRequestedProducer;
	private final RedisTemplate<String, String> redisTemplate;
	public static final String DAILY_QUIZ_RANK_KEY = "dailyQuizRank:";
	private final NotificationMetrics metrics;

	private final QuizReviewService reviewService = new QuizReviewService();
	/**
	 * 난이도별 5문제 출제 (정답 문제 제외)
	 * - mode: EASY | NORMAL | HARD | RANDOM
	 * - skillTag: study_service의 SkillTag.name()
	 * @param memberId 회원 ID
	 * @param skillTagCode - study_service의 SkillTag.name()
	 * @param mode - EASY | NORMAL | HARD | RANDOM
	 * @return 퀴즈 5문제 (정답 문제 제외)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<QuizItem> pickQuizByMode(Long memberId, String skillTagCode, String mode) {
		log.info("[QUIZ][출제][시도] memberId={}, skillTag={}, mode={}", memberId, skillTagCode, mode);

		// skillTag -> categoryId 매핑
		final Long categoryId;
		try {
			categoryId = registry.resolveOrThrow(skillTagCode);
		} catch (IllegalArgumentException e) {
			log.warn("[QUIZ][출제][거절] 알 수 없는 skillTag={} (카테고리 매핑 실패)", skillTagCode);
			throw new QuizException(ErrorCode.INVALID_SKILL_TAG, e);
		}

		// 모드에 따른 난이도 매핑
		QuizLevel level;
		String m = mode == null ? "" : mode.trim().toUpperCase();
		switch (m) {
			case "EASY"   -> level = QuizLevel.EASY;
			case "NORMAL" -> level = QuizLevel.NORMAL;
			case "HARD"   -> level = QuizLevel.HARD;
			case "RANDOM" -> level = null; // 모든 레벨 랜덤
			default -> {
				log.warn("[QUIZ][출제][거절] 지원하지 않는 모드: {}", mode);
				throw new QuizException(ErrorCode.UNSUPPORTED_MODE);
			}
		}

		// 회원이 맞춘 퀴즈 ID 조회 (출제 제외용)
		List<Long> correctIds = memberResultPort.findCorrectQuizIds(memberId);
		// 출제 (최대 5문제)
		PageRequest page5 = PageRequest.of(0, 5);
		// level이 null이면 모든 난이도에서 출제
		List<Quiz> picked = quizRepository.pick(categoryId, level, correctIds, page5);

		// 응답 변환
		List<QuizItem> items = picked.stream().map(QuizItem::from).toList();
		log.info("[QUIZ][출제][성공] memberId={}, categoryId={}, mode={}, size={}",
			memberId, categoryId, m, items.size());
		return items;
	}
	/**
	 * 퀴즈 정답 제출
	 * @param memberId 회원 ID
	 * @param req SubmitAnswersRequest
	 * @return SubmitAnswersResponse
	 */
	@Override
	@Transactional
	@Timed(value = "quiz_submit_latency")
	@Counted(value = "quiz_submit_total")
	public SubmitAnswersResponse submitAnswers(Long memberId, SubmitAnswersRequest req, Long groupId) {
		int itemCount = (req.items() == null ? 0 : req.items().size());
		log.info("[QUIZ][제출][시도] memberId={}, skillTag={}, mode={}, items={}",
			memberId, req.skillTag(), req.mode(), itemCount);

		// skillTag -> categoryId 매핑
		final Long categoryId = registry.resolveOrThrow(req.skillTag());

		// 정답 처리
		int correct = 0;
		boolean anyWrong = false;

		// 결과 리스트
		List<SubmitAnswerResult> results = new java.util.ArrayList<>();

		// 각 항목 처리
		for (SubmitAnswerItem item : req.items()) {
			Quiz quiz = quizRepository.findById(item.quizId()).orElseThrow(() -> {
				log.warn("[QUIZ][제출][거절] quizId={} 를 찾을 수 없음", item.quizId());
				return new QuizException(ErrorCode.QUIZ_NOT_FOUND);
			});

			// 카테고리 불일치 방지
			if (!quiz.getCategoryId().equals(categoryId)) {
				log.warn("[QUIZ][제출][거절] category mismatch - expected={}, actual={}, quizId={}",
					categoryId, quiz.getCategoryId(), quiz.getQuizId());
				throw new QuizException(ErrorCode.CATEGORY_MISMATCH);
			}

			// 정답 여부 판단
			boolean ok = quiz.isCorrect(item.answer());
			if (ok) correct++; else anyWrong = true;

			// 결과 저장
			results.add(new SubmitAnswerResult(
				quiz.getQuizId(),
				ok,
				item.answer(),
				quiz.getAnswer(),
				quiz.getExplain(),
				quiz.getLevel().name(),
				quiz.getCategoryId()
			));

			// 회원 퀴즈 결과 저장
			try {
				eventPublisher.publish(
					memberId,
					quiz.getQuizId(),
					quiz.getCategoryId(),
					quiz.getLevel().name(),
					item.answer(),
					ok
				);
				log.info("[QUIZ][알림][발행] memberId={}, quizId={}, correct={}", memberId, quiz.getQuizId(), ok);
				metrics.result("quiz_answer_event_result_total", "result", "success");
			} catch (Exception e) {
				log.warn("[QUIZ][알림][발행실패] memberId={}, quizId={}, err={}",
					memberId, quiz.getQuizId(), e.toString(), e);
				metrics.result("quiz_answer_event_result_total",
					"result", "error",
					"exception", e.getClass().getSimpleName()
				);
			}
		}

		// 오답이 하나라도 있었다면, 이번 제출에 대해 단 1회만 ai-review 요청 발행
		if (anyWrong) {
			try {
				aiReviewRequestedProducer.publish(
					memberId,
					categoryId,
					req.mode(),
					null,
					null
				);
				log.info("[AI-REVIEW][REQUESTED][PUBLISHED] memberId={}, categoryId={}", memberId, categoryId);
				metrics.result("ai_review_request_result_total", "result", "success");
			} catch (Exception e) {
				log.warn("[AI-REVIEW][REQUESTED][FAIL] memberId={}, categoryId={}, err={}",
					memberId, categoryId, e.toString(), e);
				metrics.result("ai_review_request_result_total",
					"result", "error",
					"exception", e.getClass().getSimpleName()
				);
			}
		}

		// 응답 생성
		SubmitAnswersResponse resp = new SubmitAnswersResponse(itemCount, correct, results);

		// 어떤 그룹의 어떤 멤버가 문제를 풀고 몇 점을 맞았는지 저장 (redis)
		// 저장: groupId 키 아래 memberId를 멤버로, 점수를 스코어로
		String key = DAILY_QUIZ_RANK_KEY + groupId;
		redisTemplate.opsForZSet().add(key, memberId.toString(), correct);

		LocalDateTime expirationTime = LocalDate.now().plusDays(1).atStartOfDay();  // 다음 날 00:00
		redisTemplate.expireAt(key, Date.from(expirationTime.atZone(ZoneId.systemDefault()).toInstant()));

		log.info("[QUIZ][제출][성공] memberId={}, total={}, correct={}", memberId, itemCount, correct);
		metrics.result("quiz_submit_result_total",
			"result", "success",
			"had_wrong", anyWrong ? "true" : "false"
		);
		return resp;
	}

	/**
	 * 회원의 퀴즈 풀이 이력 기반 출제
	 * - 틀린 문제 비율(wrongRatio)만큼 틀린 문제에서 우선 출제
	 * - 부족분은 맞은 문제에서 출제
	 * - 그래도 부족하면 카테고리 내 랜덤 출제
	 * @param memberId 회원 ID
	 * @param skillTagCode - study_service의 SkillTag.name()
	 * @param mode - EASY | NORMAL | HARD | RANDOM
	 * @param totalOpt 출제할 총 문제 수 (기본 5)
	 * @param wrongRatioOpt 틀린 문제 비율 (0.0 ~ 1.0, 기본 0.6)
	 * @return 출제된 퀴즈 목록
	 */
	@Override
	@Transactional(readOnly = true)
	public List<QuizItem> pickReviewByHistory(
		Long memberId, String skillTagCode, String mode,
		Integer totalOpt, Double wrongRatioOpt
	) {
		final Long categoryId = registry.resolveOrThrow(skillTagCode);

		final QuizLevel level = switch ((mode == null ? "" : mode.trim().toUpperCase())) {
			case "EASY" -> QuizLevel.EASY;
			case "NORMAL" -> QuizLevel.NORMAL;
			case "HARD" -> QuizLevel.HARD;
			case "RANDOM" -> null;
			default -> throw new QuizException(ErrorCode.UNSUPPORTED_MODE);
		};

		// 카테고리 내 히스토리 수집
		List<Long> wrongIds   = memberResultPort.findAnsweredQuizIds(memberId, categoryId, false);
		List<Long> correctIds = memberResultPort.findAnsweredQuizIds(memberId, categoryId, true);

		// 히스토리 합집합
		List<Long> unionIds = new ArrayList<>();
		if (wrongIds != null)   unionIds.addAll(wrongIds);
		if (correctIds != null) unionIds.addAll(correctIds);
		unionIds = unionIds.stream().distinct().toList();

		// 카테고리 내 히스토리 조회
		List<Quiz> historyInCategory = unionIds.isEmpty()
			? List.<Quiz>of()
			: quizRepository.pickFromIncludeIds(categoryId, null, unionIds, PageRequest.of(0, unionIds.size()));

		// 최소 이력 검사
		if (!reviewService.hasEnoughHistory(historyInCategory)) {
			log.warn("[QUIZ][복습][거절] 최소 이력 미달 - memberId={}, categoryId={}, history={}",
				memberId, categoryId, historyInCategory.size());
			throw new QuizException(ErrorCode.NOT_ENOUGH_HISTORY);
		}

		// 필요한 정/오답 개수 계산
		QuizReviewService.Need need = reviewService.computeNeeds(
			(totalOpt == null ? 0 : totalOpt),
			wrongRatioOpt
		);
		int total = need.wrong() + need.correct();
		int wrongNeed = need.wrong();
		int correctNeed = need.correct();

		List<Long> pickedIds = new ArrayList<>();
		List<Quiz> picked = new ArrayList<>();

		// 틀린 문제 우선
		if (wrongIds != null && !wrongIds.isEmpty() && wrongNeed > 0) {
			PageRequest page = PageRequest.of(0, wrongNeed);
			List<Quiz> fromWrong = quizRepository.pickFromIncludeIds(categoryId, level, wrongIds, page);
			picked.addAll(fromWrong);
			pickedIds.addAll(fromWrong.stream().map(Quiz::getQuizId).toList());
		}

		// 맞은 문제 보충
		int remain = total - picked.size();
		if (remain > 0 && correctIds != null && !correctIds.isEmpty()) {
			List<Long> pool = correctIds.stream().filter(id -> !pickedIds.contains(id)).toList();
			if (!pool.isEmpty()) {
				PageRequest page = PageRequest.of(0, Math.min(correctNeed, remain));
				List<Quiz> fromCorrect = quizRepository.pickFromIncludeIds(categoryId, level, pool, page);
				picked.addAll(fromCorrect);
				pickedIds.addAll(fromCorrect.stream().map(Quiz::getQuizId).toList());
			}
		}

		// 랜덤 보충
		remain = total - picked.size();
		if (remain > 0) {
			PageRequest page = PageRequest.of(0, remain);
			List<Quiz> fills = quizRepository.pickFillRandomExcluding(categoryId, level, pickedIds, page);
			picked.addAll(fills);
			pickedIds.addAll(fills.stream().map(Quiz::getQuizId).toList());
		}

		log.info("[QUIZ][복습][성공] memberId={}, categoryId={}, mode={}, total={}, wrongNeed={}, correctNeed={}, picked={}",
			memberId, categoryId, mode, total, wrongNeed, correctNeed, picked.size());

		return picked.stream().map(QuizItem::from).toList();
	}
}