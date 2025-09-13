package com.grow.notification_service.quiz.infra.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.grow.notification_service.quiz.infra.persistence.entity.QuizJpaEntity;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;

public interface QuizJpaRepository
	extends JpaRepository<QuizJpaEntity, Long> {

	/**
	 * 카테고리 ID와 질문이 주어졌을 때, 해당 퀴즈가 존재하는지 여부를 확인합니다.
	 * @param categoryId 카테고리 ID
	 * @param question 질문
	 * @return 퀴즈 존재 여부
	 */
	boolean existsByCategoryIdAndQuestion(Long categoryId, String question);

	/**
	 * 카테고리 ID, (선택적)난이도 조건에 맞는 퀴즈를 무작위로 선택합니다.
	 * 이미 정답을 맞춘 퀴즈를 제외하지 않고 단순 랜덤 출제를 할 때 사용됩니다.
	 * @param categoryId 카테고리 ID
	 * @param level 난이도 (null일 경우 모든 난이도 포함)
	 * @param pageable 페이지 정보 (예: PageRequest.of(0, 5) - 첫 페이지, 5개 항목)
	 * @return
	 */
	@Query("""
        select q from QuizJpaEntity q
        where q.categoryId = :categoryId
          and (:level is null or q.level = :level)
        order by function('rand')
    """)
	List<QuizJpaEntity> pickNoExclude(Long categoryId,
		QuizLevel level,
		Pageable pageable);

	/**
	 * 특정 카테고리 ID와 (선택적) 난이도 조건에 맞는 퀴즈를 무작위로 조회하되,
	 * 주어진 퀴즈 ID 목록은 제외합니다.
	 * 주로 "이미 맞춘 퀴즈는 다시 출제하지 않기" 용도로 사용됩니다.
	 * @param categoryId 카테고리 ID
	 * @param level 난이도 (null일 경우 모든 난이도 포함)
	 * @param excludedIds 제외할 퀴즈 ID 목록 (빈 리스트 가능)
	 * @param pageable 페이지 정보 (예: PageRequest.of(0, 5) - 첫 페이지, 5개 항목)
	 * @return 퀴즈 목록
	 */
	@Query("""
        select q from QuizJpaEntity q
        where q.categoryId = :categoryId
          and (:level is null or q.level = :level)
          and q.quizId not in :excludedIds
        order by function('rand')
    """)
	List<QuizJpaEntity> pickWithExclude(Long categoryId,
		QuizLevel level,
		List<Long> excludedIds,
		Pageable pageable);

	/**
	 * 특정 카테고리 ID와 (선택적) 난이도 조건에 맞는 퀴즈를 무작위로 조회하되,
	 * 주어진 퀴즈 ID 목록에서만 선택합니다.
	 * @param categoryId 카테고리 ID
	 * @param level 난이도 (null일 경우 모든 난이도 포함)
	 * @param includeIds 포함할 퀴즈 ID 목록 (빈 리스트 가능)
	 * @param pageable 페이지 정보 (예: PageRequest.of(0, 5) - 첫 페이지, 5개 항목)
	 * @return 퀴즈 목록
	 */
	@Query("""
    select q from QuizJpaEntity q
    where q.categoryId = :categoryId
      and (:level is null or q.level = :level)
      and q.quizId in :includeIds
    order by function('rand')
""")
	List<QuizJpaEntity> pickByIncludeIds(Long categoryId, QuizLevel level, List<Long> includeIds, Pageable pageable);

	/**
	 * 특정 카테고리 ID와 (선택적) 난이도 조건에 맞는 퀴즈를 무작위로 조회하되,
	 * 주어진 퀴즈 ID 목록은 제외합니다.
	 * @param categoryId 카테고리 ID
	 * @param level 난이도 (null일 경우 모든 난이도 포함)
	 * @param excludedIds 제외할 퀴즈 ID 목록 (빈 리스트 가능)
	 * @param pageable 페이지 정보 (예: PageRequest.of(0, 5) - 첫 페이지, 5개 항목)
	 * @return 퀴즈 목록
	 */
	@Query("""
    select q from QuizJpaEntity q
    where q.categoryId = :categoryId
      and (:level is null or q.level = :level)
      and q.quizId not in :excludedIds
    order by function('rand')
""")
	List<QuizJpaEntity> pickFillRandomExcluding(Long categoryId, QuizLevel level, List<Long> excludedIds, Pageable pageable);
}