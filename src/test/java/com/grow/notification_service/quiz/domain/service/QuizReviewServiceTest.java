package com.grow.notification_service.quiz.domain.service;

import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuizReviewServiceTest {

	private final QuizReviewService service = new QuizReviewService();

	private Quiz makeQuiz(Long id) {
		return new Quiz(
			id,
			"Q" + id,
			List.of("A", "B", "C"),
			"A",
			"exp",
			QuizLevel.EASY,
			10L
		);
	}

	@Nested
	@DisplayName("hasEnoughHistory")
	class HasEnoughHistory {

		@Test
		@DisplayName("null 이력이면 false")
		void null_history() {
			assertFalse(service.hasEnoughHistory(null));
		}

		@Test
		@DisplayName("빈 리스트면 false")
		void empty_history() {
			assertFalse(service.hasEnoughHistory(List.of()));
		}

		@Test
		@DisplayName("4개 이력이면 false (MIN_HISTORY=5 미만)")
		void four_items_false() {
			List<Quiz> history = List.of(
				makeQuiz(1L), makeQuiz(2L), makeQuiz(3L), makeQuiz(4L)
			);
			assertFalse(service.hasEnoughHistory(history));
		}

		@Test
		@DisplayName("5개 이력이면 true (임계값 충족)")
		void five_items_true() {
			List<Quiz> history = List.of(
				makeQuiz(1L), makeQuiz(2L), makeQuiz(3L), makeQuiz(4L), makeQuiz(5L)
			);
			assertTrue(service.hasEnoughHistory(history));
		}

		@Test
		@DisplayName("6개 이상이면 true")
		void six_plus_true() {
			List<Quiz> history = List.of(
				makeQuiz(1L), makeQuiz(2L), makeQuiz(3L), makeQuiz(4L), makeQuiz(5L), makeQuiz(6L)
			);
			assertTrue(service.hasEnoughHistory(history));
		}
	}

	@Nested
	@DisplayName("computeNeeds")
	class ComputeNeeds {

		@Test
		@DisplayName("total<=0 이면 DEFAULT_TOTAL(5), wrongRatio=null 이면 DEFAULT_WRONG_RATIO(0.6)")
		void defaults_applied() {
			QuizReviewService.Need need = service.computeNeeds(0, null);
			// total = 5, wrongRatio = 0.6 → wrong = ceil(3.0) = 3, correct = 2
			assertEquals(3, need.wrong());
			assertEquals(2, need.correct());
			assertEquals(5, need.wrong() + need.correct());
		}

		@Test
		@DisplayName("wrongRatio가 0.6일 때 total=10 → wrong=6, correct=4")
		void ratio_point_six_total_ten() {
			QuizReviewService.Need need = service.computeNeeds(10, 0.6);
			assertEquals(6, need.wrong());
			assertEquals(4, need.correct());
			assertEquals(10, need.wrong() + need.correct());
		}

		@Test
		@DisplayName("wrongRatio=0.5에서 올림 적용: total=7 → wrong=ceil(3.5)=4, correct=3")
		void ceiling_applied() {
			QuizReviewService.Need need = service.computeNeeds(7, 0.5);
			assertEquals(4, need.wrong());
			assertEquals(3, need.correct());
			assertEquals(7, need.wrong() + need.correct());
		}

		@Test
		@DisplayName("wrongRatio 경계값(<=0 또는 >=1)은 기본값(0.6)으로 대체")
		void out_of_range_ratio_uses_default() {
			// <= 0.0
			QuizReviewService.Need n1 = service.computeNeeds(5, 0.0);
			assertEquals(3, n1.wrong()); // default 0.6 → ceil(3)
			assertEquals(2, n1.correct());

			// >= 1.0
			QuizReviewService.Need n2 = service.computeNeeds(5, 1.0);
			assertEquals(3, n2.wrong()); // 역시 default로 처리
			assertEquals(2, n2.correct());
		}

		@Test
		@DisplayName("wrongNeed는 total을 넘지 않음, correctNeed는 음수가 되지 않음")
		void bounds_sanity() {
			QuizReviewService.Need need = service.computeNeeds(3, 0.99); // 유효 범위 → ceil(2.97)=3
			assertEquals(3, need.wrong());
			assertEquals(0, need.correct());
			assertEquals(3, need.wrong() + need.correct());
		}

		@Test
		@DisplayName("총합은 항상 total과 동일")
		void sum_matches_total() {
			QuizReviewService.Need n1 = service.computeNeeds(8, 0.25); // ceil(2) = 2
			assertEquals(8, n1.wrong() + n1.correct());

			QuizReviewService.Need n2 = service.computeNeeds(9, 0.33); // ceil(2.97)=3
			assertEquals(9, n2.wrong() + n2.correct());

			QuizReviewService.Need n3 = service.computeNeeds(1, 0.9); // ceil(0.9)=1
			assertEquals(1, n3.wrong() + n3.correct());
		}
	}
}