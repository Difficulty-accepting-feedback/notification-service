package com.grow.notification_service.quiz.application.service.impl;

import com.grow.notification_service.common.config.RedisConfig;
import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QuizException;
import com.grow.notification_service.quiz.application.dto.QuizItem;
import com.grow.notification_service.quiz.application.event.AiReviewRequestedProducer;
import com.grow.notification_service.quiz.application.event.QuizAnsweredProducer;
import com.grow.notification_service.quiz.application.mapping.SkillTagToCategoryRegistry;
import com.grow.notification_service.quiz.application.port.MemberQuizResultPort;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
@Import(RedisConfig.class)
class QuizApplicationServiceImplTest {

    @Mock
    private SkillTagToCategoryRegistry registry;
    @Mock
    private MemberQuizResultPort memberResultPort;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAnsweredProducer eventPublisher;
    @Mock
    private AiReviewRequestedProducer aiReviewRequestedProducer;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private QuizApplicationService service;

    @BeforeEach
    void setUp() {
        // opsForZSet()이 모킹된 ZSetOperations를 반환하도록 설정 (NPE 방지)
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // add() 메서드 모킹: 실제 호출 시 true 반환 (또는 필요에 따라 조정, 검증 생략)
        lenient().when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        // expireAt() 메서드 모킹: 호출 시 아무 동작 안 함 (검증 생략)
        lenient().when(redisTemplate.expireAt(anyString(), any(Date.class))).thenReturn(true);

        service = new QuizApplicationServiceImpl(
                registry,
                memberResultPort,
                quizRepository,
                eventPublisher,
                aiReviewRequestedProducer,
                redisTemplate
        );
    }

    private Quiz makeQuiz(Long id, Long categoryId, QuizLevel level, String answer) {
        // 선택지는 4개가 일반적이지만, 도메인 제약이 없다면 3개여도 테스트엔 문제 없음.
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
        @DisplayName("정상: 2개 제출, 1개 정답 → 정답/오답 이벤트 2건 + AI 리뷰 요청 1건")
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

            SubmitAnswersResponse resp = service.submitAnswers(memberId, req, 1L);

            assertEquals(2, resp.total());
            assertEquals(1, resp.correctCount());
            assertEquals(2, resp.results().size());

            // 퀴즈 정답 제출 이벤트(문항별) 2건
            verify(eventPublisher, times(2))
                    .publish(eq(memberId), anyLong(), eq(10L), anyString(), anyString(), anyBoolean());

            // 오답이 하나라도 있었으므로 AI 리뷰 요청 1건
            verify(aiReviewRequestedProducer, times(1))
                    .publish(eq(memberId), eq(10L), eq("NORMAL"), isNull(), isNull());
        }

        @Test
        @DisplayName("에러: skillTag 매핑 실패시 QuizException")
        void error_invalid_skill_tag_on_submit() {
            when(registry.resolveOrThrow("NOPE"))
                    .thenThrow(new QuizException(ErrorCode.INVALID_SKILL_TAG));
            SubmitAnswersRequest req = new SubmitAnswersRequest("NOPE", "EASY",
                    List.of(new SubmitAnswerItem(1L, "A")));
            assertThrows(QuizException.class, () -> service.submitAnswers(1L, req, 1L));

            verify(eventPublisher, never()).publish(anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyBoolean());
            verify(aiReviewRequestedProducer, never()).publish(anyLong(), anyLong(), anyString(), any(), any());
        }

        @Test
        @DisplayName("에러: quizId 조회 실패시 QuizException")
        void error_quiz_not_found() {
            when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
            when(quizRepository.findById(999L)).thenReturn(Optional.empty());

            SubmitAnswersRequest req = new SubmitAnswersRequest("JAVA_PROGRAMMING", "EASY",
                    List.of(new SubmitAnswerItem(999L, "A")));

            assertThrows(QuizException.class, () -> service.submitAnswers(1L, req, 1L));

            verify(eventPublisher, never()).publish(anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyBoolean());
            verify(aiReviewRequestedProducer, never()).publish(anyLong(), anyLong(), anyString(), any(), any());
        }

        @Test
        @DisplayName("에러: 카테고리 불일치 시 QuizException")
        void error_category_mismatch() {
            when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
            Quiz q = makeQuiz(1L, 99L, QuizLevel.EASY, "A"); // 기대 카테고리 10과 불일치
            when(quizRepository.findById(1L)).thenReturn(Optional.of(q));

            SubmitAnswersRequest req = new SubmitAnswersRequest("JAVA_PROGRAMMING", "EASY",
                    List.of(new SubmitAnswerItem(1L, "A")));

            assertThrows(QuizException.class, () -> service.submitAnswers(1L, req, 1L));

            verify(eventPublisher, never()).publish(anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyBoolean());
            verify(aiReviewRequestedProducer, never()).publish(anyLong(), anyLong(), anyString(), any(), any());
        }

        @Test
        @DisplayName("정상: 모두 오답이어도 이벤트는 2건 + AI 리뷰 요청 1건")
        void success_all_wrong_still_publish_events() {
            when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
            Quiz q1 = makeQuiz(1L, 10L, QuizLevel.EASY, "A");
            Quiz q2 = makeQuiz(2L, 10L, QuizLevel.EASY, "B");
            when(quizRepository.findById(1L)).thenReturn(Optional.of(q1));
            when(quizRepository.findById(2L)).thenReturn(Optional.of(q2));

            SubmitAnswersRequest req = new SubmitAnswersRequest("JAVA_PROGRAMMING", "EASY",
                    List.of(new SubmitAnswerItem(1L, "Z"), new SubmitAnswerItem(2L, "Z")));

            SubmitAnswersResponse resp = service.submitAnswers(123L, req, 1L);

            assertEquals(2, resp.total());
            assertEquals(0, resp.correctCount());

            verify(eventPublisher, times(2))
                    .publish(eq(123L), anyLong(), eq(10L), anyString(), anyString(), eq(false));

            // 오답 포함 → AI 리뷰 요청은 1회
            verify(aiReviewRequestedProducer, times(1))
                    .publish(eq(123L), eq(10L), eq("EASY"), isNull(), isNull());
        }
    }

    @Nested
    @DisplayName("pickReviewByHistory")
    class PickReviewByHistory {

        @Test
        @DisplayName("정상: 복습 출제 - 틀린 문제 우선, 부족분은 맞은 문제에서 보충")
        void success_review_basic() {
            Long memberId = 1L;
            String skillTag = "JAVA_PROGRAMMING";
            Long categoryId = 10L;

            when(registry.resolveOrThrow(skillTag)).thenReturn(categoryId);

            List<Long> wrongIds = List.of(1L, 2L, 3L, 4L);
            List<Long> correctIds = List.of(5L, 6L, 7L);
            when(memberResultPort.findAnsweredQuizIds(memberId, categoryId, false)).thenReturn(wrongIds);
            when(memberResultPort.findAnsweredQuizIds(memberId, categoryId, true)).thenReturn(correctIds);

            // historyInCategory (unionIds = 7개)
            List<Long> unionIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);
            List<Quiz> historyQuizzes = unionIds.stream()
                    .map(id -> makeQuiz(id, categoryId, QuizLevel.EASY, "A"))
                    .toList();
            when(quizRepository.pickFromIncludeIds(
                    eq(categoryId),
                    isNull(),
                    argThat(list -> list != null && list.containsAll(unionIds) && list.size() == unionIds.size()),
                    any(PageRequest.class))
            ).thenReturn(historyQuizzes);

            // 1) wrong 3개
            List<Quiz> fromWrong = List.of(
                    makeQuiz(1L, categoryId, QuizLevel.EASY, "A"),
                    makeQuiz(2L, categoryId, QuizLevel.EASY, "A"),
                    makeQuiz(3L, categoryId, QuizLevel.EASY, "A")
            );
            when(quizRepository.pickFromIncludeIds(
                    eq(categoryId),
                    isNull(),
                    eq(wrongIds),
                    any(PageRequest.class))
            ).thenReturn(fromWrong);

            // 2) correct 2개
            List<Quiz> fromCorrect = List.of(
                    makeQuiz(5L, categoryId, QuizLevel.NORMAL, "B"),
                    makeQuiz(6L, categoryId, QuizLevel.NORMAL, "B")
            );
            when(quizRepository.pickFromIncludeIds(
                    eq(categoryId),
                    isNull(),
                    argThat(list -> list != null
                            && list.containsAll(correctIds)
                            && list.size() == correctIds.size()),
                    any(PageRequest.class))
            ).thenReturn(fromCorrect);

            List<QuizItem> items = service.pickReviewByHistory(memberId, skillTag, "RANDOM", 5, 0.6);

            assertEquals(5, items.size());
            verify(quizRepository, never()).pickFillRandomExcluding(anyLong(), any(), anyList(), any());
        }

        @Test
        @DisplayName("에러: 복습 최소 이력(<5) 미달 시 예외")
        void error_not_enough_history() {
            Long memberId = 1L;
            String skillTag = "JAVA_PROGRAMMING";
            Long categoryId = 10L;

            when(registry.resolveOrThrow(skillTag)).thenReturn(categoryId);

            // history: wrong 2 + correct 2 = 4 (<5)
            when(memberResultPort.findAnsweredQuizIds(memberId, categoryId, false)).thenReturn(List.of(1L, 2L));
            when(memberResultPort.findAnsweredQuizIds(memberId, categoryId, true)).thenReturn(List.of(3L, 4L));

            // historyInCategory 4개로 세팅
            List<Long> unionIds = List.of(1L, 2L, 3L, 4L);
            List<Quiz> historyQuizzes = unionIds.stream()
                    .map(id -> makeQuiz(id, categoryId, QuizLevel.EASY, "A"))
                    .toList();
            when(quizRepository.pickFromIncludeIds(eq(categoryId), isNull(), eq(unionIds), any(PageRequest.class)))
                    .thenReturn(historyQuizzes);

            assertThrows(QuizException.class,
                    () -> service.pickReviewByHistory(memberId, skillTag, "RANDOM", 5, 0.6));

            verify(quizRepository, never()).pickFromIncludeIds(eq(categoryId), isNull(), eq(List.of(1L, 2L)), any());
            verify(quizRepository, never()).pickFillRandomExcluding(anyLong(), any(), anyList(), any());
        }

        @Test
        @DisplayName("정상: 틀린 문제 부족 → 랜덤 보충으로 채움")
        void success_fill_random_when_wrong_short() {
            Long memberId = 2L;
            String skillTag = "JAVA_PROGRAMMING";
            Long categoryId = 10L;

            when(registry.resolveOrThrow(skillTag)).thenReturn(categoryId);

            // 틀린 1개, 맞은 5개 → unionIds = 6개 (최소 이력 충족)
            List<Long> wrongIds = List.of(1L);
            List<Long> correctIds = List.of(9L, 10L, 11L, 12L, 13L);
            when(memberResultPort.findAnsweredQuizIds(memberId, categoryId, false)).thenReturn(wrongIds);
            when(memberResultPort.findAnsweredQuizIds(memberId, categoryId, true)).thenReturn(correctIds);

            List<Long> unionIds = new java.util.ArrayList<>();
            unionIds.addAll(wrongIds);
            unionIds.addAll(correctIds);
            List<Quiz> historyQuizzes = unionIds.stream()
                    .map(id -> makeQuiz(id, categoryId, QuizLevel.NORMAL, "C"))
                    .toList();
            when(quizRepository.pickFromIncludeIds(
                    eq(categoryId),
                    isNull(),
                    argThat(list -> list != null && list.containsAll(unionIds) && list.size() == unionIds.size()),
                    any(PageRequest.class))
            ).thenReturn(historyQuizzes);

            // total=5, wrongRatio=0.6 → wrongNeed=3, correctNeed=2
            // 1) 틀린에서 1개만
            List<Quiz> fromWrong = List.of(makeQuiz(1L, categoryId, QuizLevel.NORMAL, "C"));
            when(quizRepository.pickFromIncludeIds(
                    eq(categoryId),
                    isNull(),
                    eq(wrongIds),
                    any(PageRequest.class))
            ).thenReturn(fromWrong);

            // 2) 맞은 문제에서 2개 보충
            List<Quiz> fromCorrect = List.of(
                    makeQuiz(9L, categoryId, QuizLevel.EASY, "A"),
                    makeQuiz(10L, categoryId, QuizLevel.EASY, "A")
            );
            when(quizRepository.pickFromIncludeIds(
                    eq(categoryId),
                    isNull(),
                    argThat(list -> list != null
                            && list.containsAll(correctIds)
                            && list.size() == correctIds.size()),
                    any(PageRequest.class))
            ).thenReturn(fromCorrect);

            // 3) 랜덤 보충 2개
            List<Quiz> fills = List.of(
                    makeQuiz(101L, categoryId, QuizLevel.EASY, "A"),
                    makeQuiz(102L, categoryId, QuizLevel.EASY, "A")
            );
            when(quizRepository.pickFillRandomExcluding(eq(categoryId), isNull(), anyList(), any(PageRequest.class)))
                    .thenReturn(fills);

            List<QuizItem> items = service.pickReviewByHistory(memberId, skillTag, "RANDOM", 5, 0.6);

            assertEquals(5, items.size());
            verify(quizRepository).pickFromIncludeIds(eq(categoryId), isNull(), eq(wrongIds), any(PageRequest.class));
            verify(quizRepository).pickFromIncludeIds(
                    eq(categoryId),
                    isNull(),
                    argThat(list -> list != null
                            && list.containsAll(correctIds)
                            && list.size() == correctIds.size()),
                    any(PageRequest.class)
            );
            verify(quizRepository).pickFillRandomExcluding(eq(categoryId), isNull(), anyList(), any(PageRequest.class));
        }

        @Test
        @DisplayName("에러: 지원하지 않는 모드이면 QuizException (복습)")
        void error_unsupported_mode_on_review() {
            when(registry.resolveOrThrow("JAVA_PROGRAMMING")).thenReturn(10L);
            assertThrows(QuizException.class,
                    () -> service.pickReviewByHistory(1L, "JAVA_PROGRAMMING", "INSANE", 5, 0.6));
            verify(quizRepository, never()).pickFromIncludeIds(anyLong(), any(), anyList(), any());
        }
    }
}