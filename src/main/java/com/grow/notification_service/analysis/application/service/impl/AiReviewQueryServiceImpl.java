package com.grow.notification_service.analysis.application.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.application.service.AiReviewQueryService;
import com.grow.notification_service.analysis.infra.persistence.entity.MemberLatestAiReviewId;
import com.grow.notification_service.analysis.infra.persistence.repository.MemberLatestAiReviewJpaRepository;
import com.grow.notification_service.quiz.application.dto.QuizItem;
import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewQueryServiceImpl implements AiReviewQueryService {

	private final MemberLatestAiReviewJpaRepository latestRepo;
	private final QuizRepository quizRepository;
	private final ObjectMapper mapper;

	/**
	 * 멤버별, 카테고리별 최신 AI 복습 퀴즈 목록 조회
	 * @param memberId 멤버 ID
	 * @param categoryId 카테고리 ID
	 * @param sizeOpt 최대 개수 (null 또는 0 이하일 경우 기본값 5)
	 * @return 퀴즈 목록 (없을 경우 빈 리스트)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<QuizItem> getLatestGenerated(Long memberId, Long categoryId, Integer sizeOpt) {
		MemberLatestAiReviewId id = new MemberLatestAiReviewId(memberId, categoryId);

		return latestRepo.findById(id)
			.map(row -> {
				List<Long> ids = parseIds(row.getQuizIdsJson());
				if (ids.isEmpty()) {
					return Collections.<QuizItem>emptyList();
				}

				// 요청된 개수 또는 기본값
				int requested = (sizeOpt == null || sizeOpt <= 0) ? 5 : sizeOpt;
				int size = Math.min(requested, ids.size());

				// 최신 퀴즈 ID 기준으로 자르기
				List<Long> clipped = (ids.size() > size)
					? ids.subList(ids.size() - size, ids.size())
					: ids;

				List<Quiz> quizzes = quizRepository.findByIds(clipped);

				Map<Long, Quiz> byId = quizzes.stream()
					.collect(Collectors.toMap(Quiz::getQuizId, q -> q, (a, b) -> a));

				// clipped 순서를 유지하여 DTO 변환
				return clipped.stream()
					.map(byId::get)
					.filter(Objects::nonNull)
					.map(QuizItem::from)
					.collect(Collectors.toList());
			})
			.orElseGet(Collections::<QuizItem>emptyList);
	}

	/**
	 * 퀴즈 ID 리스트 JSON 파싱
	 * @param json 퀴즈 ID 리스트 JSON
	 * @return 파싱된 퀴즈 ID 리스트 (실패 시 빈 리스트)
	 */
	private List<Long> parseIds(String json) {
		try {
			return mapper.readValue(json, new TypeReference<List<Long>>() {});
		} catch (Exception e) {
			log.warn("[AI-REVIEW][QUERY] quizIdsJson 파싱 실패: {}", json, e);
			return Collections.emptyList();
		}
	}
}