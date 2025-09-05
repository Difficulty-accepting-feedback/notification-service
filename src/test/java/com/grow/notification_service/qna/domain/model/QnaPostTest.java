package com.grow.notification_service.qna.domain.model;

import com.grow.notification_service.qna.domain.exception.QnaDomainException;
import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("[Domain][QnA] QnaPost 단위 테스트 - GROW(DDD)")
class QnaPostTest {

	private static final Clock FIXED_CLOCK =
		Clock.fixed(Instant.parse("2025-09-04T12:34:56Z"), ZoneId.of("UTC"));

	@Nested
	@DisplayName("질문(루트) 생성 규칙")
	class NewRootQuestionTest {

		@Test
		@DisplayName("성공: 루트 질문은 parentId=null, type=QUESTION, status=ACTIVE, createdAt=now(clock)")
		void newRootQuestion_success() {
			Long authorId = 100L;
			String content = "결제 실패 보상 트랜잭션 구조 문의";

			QnaPost q = QnaPost.newRootQuestion(authorId, content, FIXED_CLOCK);

			assertAll(
				() -> assertNull(q.getId()),
				() -> assertEquals(QnaType.QUESTION, q.getType()),
				() -> assertNull(q.getParentId()),
				() -> assertEquals(authorId, q.getMemberId()),
				() -> assertEquals(content, q.getContent()),
				() -> assertEquals(QnaStatus.ACTIVE, q.getStatus()),
				() -> assertEquals(LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone()), q.getCreatedAt()),
				() -> assertNull(q.getUpdatedAt())
			);
		}

		@Test
		@DisplayName("실패: 내용이 공백/빈값이면 예외")
		void newRootQuestion_blankContent_throws() {
			assertThrows(QnaDomainException.class,
				() -> QnaPost.newRootQuestion(1L, "   ", FIXED_CLOCK));
			assertThrows(QnaDomainException.class,
				() -> QnaPost.newRootQuestion(1L, "", FIXED_CLOCK));
			assertThrows(QnaDomainException.class,
				() -> QnaPost.newRootQuestion(1L, null, FIXED_CLOCK));
		}
	}

	@Nested
	@DisplayName("답변 생성 규칙")
	class NewAnswerTest {

		@Test
		@DisplayName("성공: 답변은 type=ANSWER, parentId=질문ID, status=ACTIVE")
		void newAnswer_success() {
			Long authorId = 200L;
			Long questionId = 10L;
			String content = "보상 트랜잭션은 REQUIRES_NEW로 분리합니다.";

			QnaPost a = QnaPost.newAnswer(authorId, questionId, content, FIXED_CLOCK);

			assertAll(
				() -> assertNull(a.getId()),
				() -> assertEquals(QnaType.ANSWER, a.getType()),
				() -> assertEquals(questionId, a.getParentId()),
				() -> assertEquals(authorId, a.getMemberId()),
				() -> assertEquals(content, a.getContent()),
				() -> assertEquals(QnaStatus.ACTIVE, a.getStatus()),
				() -> assertEquals(LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone()), a.getCreatedAt()),
				() -> assertNull(a.getUpdatedAt())
			);
		}

		@Test
		@DisplayName("실패: 답변 생성 시 parentId가 없으면 예외")
		void newAnswer_withoutParent_throws() {
			assertThrows(QnaDomainException.class,
				() -> new QnaPost(null, QnaType.ANSWER, null, 1L, "a", QnaStatus.ACTIVE,
					LocalDateTime.now(FIXED_CLOCK), null));
		}

		@Test
		@DisplayName("실패: 내용이 공백/빈값이면 예외")
		void newAnswer_blankContent_throws() {
			assertThrows(QnaDomainException.class,
				() -> QnaPost.newAnswer(1L, 10L, "   ", FIXED_CLOCK));
		}
	}

	@Nested
	@DisplayName("추가 질문(꼬리질문) 생성 규칙")
	class NewFollowUpQuestionTest {

		@Test
		@DisplayName("성공: 추가 질문은 type=QUESTION, parentId=ANSWER의 id (체인형 구조)")
		void newFollowUp_success() {
			Long memberId = 300L;
			Long answerId = 11L;
			String content = "그럼 멱등키는 어디에 저장하나요?";

			QnaPost f = QnaPost.newFollowUpQuestion(memberId, answerId, content, FIXED_CLOCK);

			assertAll(
				() -> assertNull(f.getId()),
				() -> assertEquals(QnaType.QUESTION, f.getType()),
				() -> assertEquals(answerId, f.getParentId()),
				() -> assertEquals(memberId, f.getMemberId()),
				() -> assertEquals(content, f.getContent()),
				() -> assertEquals(QnaStatus.ACTIVE, f.getStatus()),
				() -> assertEquals(LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone()), f.getCreatedAt()),
				() -> assertNull(f.getUpdatedAt())
			);
		}

		@Test
		@DisplayName("실패: 내용이 공백/빈값이면 예외")
		void newFollowUp_blankContent_throws() {
			assertThrows(QnaDomainException.class,
				() -> QnaPost.newFollowUpQuestion(1L, 11L, " ", FIXED_CLOCK));
		}
	}

	@Nested
	@DisplayName("불변성/파생 생성자")
	class WithParentIdTest {

		@Test
		@DisplayName("withParentId는 기존 값을 보존하고 parentId만 변경한 새 인스턴스를 반환한다")
		void withParentId_returnsNewInstance() {
			QnaPost original = QnaPost.newRootQuestion(1L, "원 질문", FIXED_CLOCK);

			QnaPost changed = original.withParentId(99L);

			// 새 인스턴스
			assertNotSame(original, changed);

			// parentId만 변경됨
			assertAll(
				() -> assertEquals(original.getId(), changed.getId()),
				() -> assertEquals(original.getType(), changed.getType()),
				() -> assertEquals(99L, changed.getParentId()),
				() -> assertEquals(original.getMemberId(), changed.getMemberId()),
				() -> assertEquals(original.getContent(), changed.getContent()),
				() -> assertEquals(original.getStatus(), changed.getStatus()),
				() -> assertEquals(original.getCreatedAt(), changed.getCreatedAt()),
				() -> assertEquals(original.getUpdatedAt(), changed.getUpdatedAt())
			);
		}
	}

	@Nested
	@DisplayName("체인형 스레드 상태 규칙")
	class ThreadRuleTest {

		@Test
		@DisplayName("질문 추가 시 스레드는 ACTIVE")
		void questionAdded_rule() {
			assertEquals(QnaStatus.ACTIVE, QnaPost.threadStatusOnQuestionAdded());
		}

		@Test
		@DisplayName("답변 추가 시 스레드는 COMPLETED")
		void answerAdded_rule() {
			assertEquals(QnaStatus.COMPLETED, QnaPost.threadStatusOnAnswerAdded());
		}
	}

	@Nested
	@DisplayName("생성자 직접 호출 유효성")
	class CtorValidationTest {

		@Test
		@DisplayName("성공: QUESTION은 parentId=null이어도 허용")
		void ctor_question_parentNull_ok() {
			QnaPost p = new QnaPost(
				null,
				QnaType.QUESTION,
				null,
				1L,
				"도메인 예외 처리 정책?",
				QnaStatus.ACTIVE,
				LocalDateTime.now(FIXED_CLOCK),
				null
			);
			assertEquals(QnaType.QUESTION, p.getType());
			assertNull(p.getParentId());
		}

		@Test
		@DisplayName("실패: content가 null/blank면 예외")
		void ctor_blank_throws() {
			assertThrows(QnaDomainException.class,
				() -> new QnaPost(null, QnaType.QUESTION, null, 1L, " ", QnaStatus.ACTIVE,
					LocalDateTime.now(FIXED_CLOCK), null));
		}
	}
}