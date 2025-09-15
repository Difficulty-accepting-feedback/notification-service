package com.grow.notification_service.quiz.infra.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;
import com.grow.notification_service.quiz.infra.persistence.entity.QuizJpaEntity;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import com.grow.notification_service.quiz.infra.persistence.mapper.QuizMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QuizRepositoryImpl implements QuizRepository {

	private final QuizJpaRepository jpa;
	private final QuizMapper mapper;

	/**
	 * 퀴즈 저장
	 * @param quiz 저장할 퀴즈 도메인 객체
	 * @return 저장된 퀴즈 도메인 객체 (ID 포함)
	 */
	@Override
	public Quiz save(Quiz quiz) {
		QuizJpaEntity saved = jpa.save(mapper.toEntity(quiz));
		return mapper.toDomain(saved);
	}

	/**
	 * 퀴즈 ID로 조회
	 * @param quizId 조회할 퀴즈 ID
	 * @return 퀴즈 도메인 객체 (존재하지 않으면 빈 Optional)
	 */
	@Override
	public Optional<Quiz> findById(Long quizId) {
		return jpa.findById(quizId).map(mapper::toDomain);
	}

	/**
	 * 카테고리 ID와 문제로 존재 여부 확인
	 * @param categoryId 카테고리 ID
	 * @param question 문제
	 * @return 존재하면 true, 없으면 false
	 */
	@Override
	public boolean existsByCategoryIdAndQuestion(Long categoryId, String question) {
		return jpa.existsByCategoryIdAndQuestion(categoryId, question);
	}

	/**
	 * 퀴즈 출제
	 * - 특정 카테고리, 난이도, 제외할 퀴즈 ID 목록, 페이지 정보에 따라 퀴즈 목록 반환
	 * @param categoryId 카테고리 ID
	 * @param level 난이도 (EASY, NORMAL, HARD)
	 * @param excludedIds 제외할 퀴즈 ID 목록 (null 또는 빈 리스트 가능)
	 * @param pageable 페이지 정보 (페이지 번호, 페이지 크기)
	 * @return 조건에 맞는 퀴즈 도메인 객체 목록
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Quiz> pick(Long categoryId, QuizLevel level, List<Long> excludedIds, Pageable pageable) {
		boolean empty = (excludedIds == null || excludedIds.isEmpty());
		List<QuizJpaEntity> rows;
		if (empty) {
			rows = jpa.pickNoExclude(categoryId, level, pageable);
		} else {
			rows = jpa.pickWithExclude(categoryId, level, excludedIds, pageable);
		}
		return rows.stream().map(mapper::toDomain).toList();
	}

	/**
	 * 특정 퀴즈 ID 목록에서 무작위로 퀴즈 선택
	 * @param categoryId 카테고리 ID
	 * @param level 난이도 (null이면 모든 난이도)
	 * @param includeIds 포함할 퀴즈 ID 목록 (null 또는 빈 리스트이면 빈 리스트 반환)
	 * @param pageable 페이지 정보 (예: PageRequest.of(0, 5) - 첫 페이지, 5개 항목)
	 * @return 퀴즈 도메인 객체 목록
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Quiz> pickFromIncludeIds(Long categoryId, QuizLevel level, List<Long> includeIds, Pageable pageable) {
		if (includeIds == null || includeIds.isEmpty()) return List.of();
		return jpa.pickByIncludeIds(categoryId, level, includeIds, pageable)
			.stream()
			.map(mapper::toDomain)
			.toList();
	}

	/**
	 * 특정 카테고리와 난이도에서, 제외할 퀴즈 ID 목록을 제외하고 무작위로 퀴즈 선택
	 * @param categoryId 카테고리 ID
	 * @param level 난이도 (null이면 모든 난이도)
	 * @param excludedIds 제외할 퀴즈 ID 목록 (null 또는 빈 리스트 가능)
	 * @param pageable 페이지 정보 (예: PageRequest.of(0, 5) - 첫 페이지, 5개 항목)
	 * @return 퀴즈 도메인 객체 목록
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Quiz> pickFillRandomExcluding(Long categoryId, QuizLevel level, List<Long> excludedIds, Pageable pageable) {
		List<Long> excluded = (excludedIds == null) ? List.of() : excludedIds;
		return jpa.pickFillRandomExcluding(categoryId, level, excluded, pageable)
			.stream()
			.map(mapper::toDomain)
			.toList();
	}

	/**
	 * 퀴즈 ID 목록으로 퀴즈 조회
	 * @param quizIds 조회할 퀴즈 ID 목록
	 * @return 퀴즈 도메인 객체 목록 (존재하지 않는 ID는 무시)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Quiz> findByIds(List<Long> quizIds) {
		if (quizIds == null || quizIds.isEmpty()) return List.of();
		return jpa.findAllById(quizIds).stream().map(mapper::toDomain).toList();
	}
}