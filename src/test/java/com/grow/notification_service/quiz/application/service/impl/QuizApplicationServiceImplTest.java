package com.grow.notification_service.quiz.application.service.impl;

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
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersRequest;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class QuizApplicationServiceImplTest {

	@Mock private SkillTagToCategoryRegistry registry;
	@Mock private MemberQuizResultPort memberResultPort;
	@Mock private QuizRepository quizRepository;
	@Mock private QuizAnsweredProducer eventPublisher;

	private QuizApplicationService service;

	@BeforeEach
	void setUp() {
		service = new QuizApplicationServiceImpl(registry, memberResultPort, quizRepository, eventPublisher);
	}

	private Quiz makeQuiz(Long id, Long categoryId, QuizLevel level, String answer) {
		return new Quiz(
			id,
			"Q" + id,
			List.of("A", "B", "C"),
			answer,
			"exp",
			level,
			categoryId
		);
	}

	@Nested
	@DisplayName("pickQuizByMode")
	class PickQuizByMode {

		@Test
		@DisplayName("정상: EASY 모드에서 5개 출제, 이미 맞춘 문제 제외")
		void success_easy_mode() {
			Long memberId = 1L;
			String skillTag = "JAVA_PROGRAMMING";
			String mode = "EASY";

			when(registry.resolveOrThrow(skillTag)).thenReturn(10L);
			when(memberResultPort.findCorrectQuizIds(memberId)).thenReturn(List.of(100L, 200L));

			List<Quiz> picked = List.of(
				makeQuiz(1L, 10L, QuizLevel.EASY, "A"),
				makeQuiz(2L, 10L, QuizLevel.EASY, "B")
			);
			when(quizRepository.pick(eq(10L), eq(QuizLevel.EASY), eq(List.of(100L, 200L)), any(PageRequest.class)))
				.thenReturn(picked);

			List<QuizItem> items = service.pickQuizByMode(memberId, skillTag, mode);

			assertEquals(2, items.size());
			assertEquals(1L, items.get(0).quizId());
			assertEquals("Q1", items.get(0).question());
			verify(registry).resolveOrThrow(skillTag);
			verify(memberResultPort).findCorrectQuizIds(memberId);
			verify(quizRepository).pick(eq(10L), eq(QuizLevel.EASY), eq(List.of(100L, 200L)), any(PageRequest.class));
		}

		@Test
		@DisplayName("에러: 지원하지 않는 모드이면 QuizException")
		void error_unsupported_mode() {
			when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
			assertThrows(QuizException.class,
				() -> service.pickQuizByMode(1L, "JAVA_PROGRAMMING", "INSANE"));
			verify(quizRepository, never()).pick(anyLong(), any(), anyList(), any());
		}

		@Test
		@DisplayName("에러: skillTag 매핑 실패시 QuizException")
		void error_invalid_skill_tag() {
			when(registry.resolveOrThrow("NOPE"))
				.thenThrow(new QuizException(ErrorCode.INVALID_SKILL_TAG));
			assertThrows(QuizException.class, () -> service.pickQuizByMode(1L, "NOPE", "EASY"));
			verify(quizRepository, never()).pick(anyLong(), any(), anyList(), any());
		}

		@Test
		@DisplayName("정상: RANDOM 모드(level=null)")
		void success_random_mode() {
			when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
			when(memberResultPort.findCorrectQuizIds(1L)).thenReturn(List.of());
			when(quizRepository.pick(eq(10L), isNull(), eq(List.of()), any(PageRequest.class)))
				.thenReturn(List.of(makeQuiz(5L, 10L, QuizLevel.HARD, "C")));

			List<QuizItem> items = service.pickQuizByMode(1L, "JAVA_PROGRAMMING", "RANDOM");

			assertEquals(1, items.size());
			assertEquals(5L, items.get(0).quizId());
			verify(quizRepository).pick(eq(10L), isNull(), eq(List.of()), any(PageRequest.class));
		}
	}

	@Nested
	@DisplayName("submitAnswers")
	class SubmitAnswers {

		@Test
		@DisplayName("정상: 2개 제출, 1개 정답 → 이벤트 2건 발행")
		void success_submit_two_items() {
			Long memberId = 77L;
			String skillTag = "JAVA_PROGRAMMING";
			when(registry.resolveOrThrow(skillTag)).thenReturn(10L);

			Quiz q1 = makeQuiz(1L, 10L, QuizLevel.NORMAL, "B");
			Quiz q2 = makeQuiz(2L, 10L, QuizLevel.NORMAL, "A");
			when(quizRepository.findById(1L)).thenReturn(Optional.of(q1));
			when(quizRepository.findById(2L)).thenReturn(Optional.of(q2));

			SubmitAnswerItem i1 = new SubmitAnswerItem(1L, "B"); // correct
			SubmitAnswerItem i2 = new SubmitAnswerItem(2L, "C"); // wrong
			SubmitAnswersRequest req = new SubmitAnswersRequest(skillTag, "NORMAL", Arrays.asList(i1, i2));

			SubmitAnswersResponse resp = service.submitAnswers(memberId, req);

			assertEquals(2, resp.total());
			assertEquals(1, resp.correctCount());
			assertEquals(2, resp.results().size());

			verify(eventPublisher, times(2))
				.publish(eq(memberId), anyLong(), eq(10L), anyString(), anyString(), anyBoolean());
		}

		@Test
		@DisplayName("에러: skillTag 매핑 실패시 QuizException")
		void error_invalid_skill_tag_on_submit() {
			when(registry.resolveOrThrow("NOPE"))
				.thenThrow(new QuizException(ErrorCode.INVALID_SKILL_TAG));
			SubmitAnswersRequest req = new SubmitAnswersRequest("NOPE", "EASY",
				List.of(new SubmitAnswerItem(1L, "A")));
			assertThrows(QuizException.class, () -> service.submitAnswers(1L, req));
			verify(eventPublisher, never()).publish(anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyBoolean());
		}

		@Test
		@DisplayName("에러: quizId 조회 실패시 QuizException")
		void error_quiz_not_found() {
			when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
			when(quizRepository.findById(999L)).thenReturn(Optional.empty());

			SubmitAnswersRequest req = new SubmitAnswersRequest("JAVA_PROGRAMMING", "EASY",
				List.of(new SubmitAnswerItem(999L, "A")));

			assertThrows(QuizException.class, () -> service.submitAnswers(1L, req));
			verify(eventPublisher, never()).publish(anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyBoolean());
		}

		@Test
		@DisplayName("에러: 카테고리 불일치 시 QuizException")
		void error_category_mismatch() {
			when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
			Quiz q = makeQuiz(1L, 99L, QuizLevel.EASY, "A"); // 기대 카테고리 10과 불일치
			when(quizRepository.findById(1L)).thenReturn(Optional.of(q));

			SubmitAnswersRequest req = new SubmitAnswersRequest("JAVA_PROGRAMMING", "EASY",
				List.of(new SubmitAnswerItem(1L, "A")));

			assertThrows(QuizException.class, () -> service.submitAnswers(1L, req));
			verify(eventPublisher, never()).publish(anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyBoolean());
		}

		@Test
		@DisplayName("정상: 모두 오답이어도 이벤트는 발행된다")
		void success_all_wrong_still_publish_events() {
			when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
			Quiz q1 = makeQuiz(1L, 10L, QuizLevel.EASY, "A");
			Quiz q2 = makeQuiz(2L, 10L, QuizLevel.EASY, "B");
			when(quizRepository.findById(1L)).thenReturn(Optional.of(q1));
			when(quizRepository.findById(2L)).thenReturn(Optional.of(q2));

			SubmitAnswersRequest req = new SubmitAnswersRequest("JAVA_PROGRAMMING", "EASY",
				List.of(new SubmitAnswerItem(1L, "Z"), new SubmitAnswerItem(2L, "Z")));

			SubmitAnswersResponse resp = service.submitAnswers(123L, req);

			assertEquals(2, resp.total());
			assertEquals(0, resp.correctCount());
			verify(eventPublisher, times(2))
				.publish(eq(123L), anyLong(), eq(10L), anyString(), anyString(), eq(false));
		}
	}
}