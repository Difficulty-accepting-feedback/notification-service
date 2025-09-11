package com.grow.notification_service.quiz.application.service.impl;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QuizException;
import com.grow.notification_service.quiz.application.MemberQuizResultPort;
import com.grow.notification_service.quiz.application.dto.QuizItem;
import com.grow.notification_service.quiz.application.event.QuizAnsweredProducer;
import com.grow.notification_service.quiz.application.mapping.SkillTagToCategoryRegistry;
import com.grow.notification_service.quiz.application.service.QuizApplicationService;
import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswerItem;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswerResult;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersRequest;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersResponse;

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
	public SubmitAnswersResponse submitAnswers(Long memberId, SubmitAnswersRequest req) {
		int itemCount = (req.items() == null ? 0 : req.items().size());
		log.info("[QUIZ][제출][시도] memberId={}, skillTag={}, mode={}, items={}",
			memberId, req.skillTag(), req.mode(), itemCount);

		// skillTag -> categoryId 매핑
		final Long categoryId;
		try {
			categoryId = registry.resolveOrThrow(req.skillTag());
		} catch (IllegalArgumentException e) {
			log.warn("[QUIZ][제출][거절] 알 수 없는 skillTag={} (카테고리 매핑 실패)", req.skillTag());
			throw new QuizException(ErrorCode.INVALID_SKILL_TAG, e);
		}

		// 정답 처리
		int correct = 0;

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
			if (ok) correct++;

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
			} catch (Exception e) {
				log.warn("[QUIZ][알림][발행실패] memberId={}, quizId={}, err={}",
					memberId, quiz.getQuizId(), e.toString(), e);
			}
		}

		// 응답 생성
		SubmitAnswersResponse resp = new SubmitAnswersResponse(itemCount, correct, results);
		log.info("[QUIZ][제출][성공] memberId={}, total={}, correct={}", memberId, itemCount, correct);
		return resp;
	}
}