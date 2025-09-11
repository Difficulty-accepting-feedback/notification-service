package com.grow.notification_service.quiz.domain.model;

import com.grow.notification_service.quiz.domain.exception.QuizDomainException;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuizTest {

	@Test
	@DisplayName("정상 생성: 모든 필드가 올바르게 매핑된다")
	void create_success() {
		Quiz quiz = Quiz.create(
			"자바의 특징은?",
			List.of("객체지향", "절차지향"),
			"객체지향",
			"자바는 객체지향 언어입니다",
			QuizLevel.EASY,
			1L
		);

		assertNull(quiz.getQuizId());
		assertEquals("자바의 특징은?", quiz.getQuestion());
		assertEquals(List.of("객체지향", "절차지향"), quiz.getChoices());
		assertEquals("객체지향", quiz.getAnswer());
		assertEquals("자바는 객체지향 언어입니다", quiz.getExplain());
		assertEquals(QuizLevel.EASY, quiz.getLevel());
		assertEquals(1L, quiz.getCategoryId());
	}

	@Test
	@DisplayName("예외: 질문이 null 또는 빈 문자열일 경우")
	void create_fail_emptyQuestion() {
		assertThrows(QuizDomainException.class,
			() -> Quiz.create("", List.of("A"), "A", "exp", QuizLevel.EASY, 1L));
	}

	@Test
	@DisplayName("예외: 보기(choices)가 null이거나 비어있을 경우")
	void create_fail_emptyChoices() {
		assertThrows(QuizDomainException.class,
			() -> Quiz.create("Q", List.of(), "A", "exp", QuizLevel.EASY, 1L));
	}

	@Test
	@DisplayName("예외: 정답이 보기에 없는 경우")
	void create_fail_answerNotInChoices() {
		assertThrows(QuizDomainException.class,
			() -> Quiz.create("Q", List.of("A", "B"), "C", "exp", QuizLevel.EASY, 1L));
	}

	@Test
	@DisplayName("예외: 레벨이 null인 경우")
	void create_fail_levelRequired() {
		assertThrows(QuizDomainException.class,
			() -> Quiz.create("Q", List.of("A"), "A", "exp", null, 1L));
	}

	@Test
	@DisplayName("예외: 카테고리 ID가 null인 경우")
	void create_fail_categoryRequired() {
		assertThrows(QuizDomainException.class,
			() -> Quiz.create("Q", List.of("A"), "A", "exp", QuizLevel.EASY, null));
	}

	@Test
	@DisplayName("정답 검증: 대소문자 무시, 공백 제거")
	void isCorrect_caseInsensitiveTrimmed() {
		Quiz quiz = Quiz.create("Q", List.of("YES", "NO"), "YES", "exp", QuizLevel.NORMAL, 2L);

		assertTrue(quiz.isCorrect("yes"));
		assertTrue(quiz.isCorrect(" YES "));
		assertFalse(quiz.isCorrect("no"));
		assertFalse(quiz.isCorrect(null));
	}

	@Test
	@DisplayName("생성자 직접 호출 시 choices는 불변 리스트로 감싸진다")
	void constructor_defensiveCopy_choicesImmutable() {
		Quiz quiz = new Quiz(1L, "Q", List.of("A", "B"), "A", "exp", QuizLevel.HARD, 3L);

		assertThrows(UnsupportedOperationException.class,
			() -> quiz.getChoices().add("C"));
	}
}