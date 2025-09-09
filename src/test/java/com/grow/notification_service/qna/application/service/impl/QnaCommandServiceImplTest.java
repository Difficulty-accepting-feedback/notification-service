package com.grow.notification_service.qna.application.service.impl;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QnaException;
import com.grow.notification_service.qna.application.event.QnaNotificationProducer;
import com.grow.notification_service.qna.application.port.AuthorityCheckerPort;
import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.domain.repository.QnaPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[App][QnA] QnaCommandServiceImpl 단위 테스트")
class QnaCommandServiceImplTest {

	@Mock
	QnaPostRepository repository;

	@Mock
	AuthorityCheckerPort authorityCheckerPort;

	@Mock
	QnaNotificationProducer qnaNotificationProducer;

	Clock fixedClock;

	QnaCommandServiceImpl service;

	@BeforeEach
	void setUp() {
		fixedClock = Clock.fixed(Instant.parse("2025-09-04T12:00:00Z"), ZoneId.of("UTC"));
		service = new QnaCommandServiceImpl(repository, authorityCheckerPort, fixedClock, qnaNotificationProducer);
	}

	@Nested
	@DisplayName("createQuestion - 질문 생성")
	class CreateQuestion {

		@Test
		@DisplayName("성공: 루트 질문 생성 (parentId=null) → 저장 & 상태 업데이트 없음")
		void create_root_question_success() {
			Long memberId = 10L;
			String content = "게이트웨이에서 권한을 어떻게 전달하나요?";
			when(repository.save(any(QnaPost.class))).thenReturn(100L);

			Long postId = service.createQuestion(memberId, content, null);

			assertThat(postId).isEqualTo(100L);
			ArgumentCaptor<QnaPost> cap = ArgumentCaptor.forClass(QnaPost.class);
			verify(repository).save(cap.capture());
			QnaPost saved = cap.getValue();

			assertAll(
				() -> assertNull(saved.getId()),
				() -> assertEquals(QnaType.QUESTION, saved.getType()),
				() -> assertNull(saved.getParentId()),
				() -> assertEquals(memberId, saved.getMemberId()),
				() -> assertEquals(content, saved.getContent()),
				() -> assertEquals(QnaStatus.ACTIVE, saved.getStatus()),
				() -> assertEquals(LocalDateTime.ofInstant(fixedClock.instant(), fixedClock.getZone()), saved.getCreatedAt()),
				() -> assertNull(saved.getUpdatedAt())
			);

			verify(repository, never()).updateRootStatusFrom(anyLong(), any());

			// 질문 생성은 알림 발행 대상 아님
			verifyNoInteractions(qnaNotificationProducer);
		}

		@Test
		@DisplayName("성공: 추가 질문 생성 (parent=ANSWER) → 루트 상태 ACTIVE로 토글")
		void create_followup_question_success() {
			Long memberId = 20L;
			Long answerId = 200L;
			String content = "카프카로 이벤트 전파하면 되나요?";

			QnaPost parentAnswer = new QnaPost(
				answerId, QnaType.ANSWER, 10L, 999L, "부모 답변",
				QnaStatus.ACTIVE,
				LocalDateTime.ofInstant(fixedClock.instant(), fixedClock.getZone()),
				null
			);

			when(repository.findById(answerId)).thenReturn(Optional.of(parentAnswer));
			when(repository.save(any(QnaPost.class))).thenReturn(300L);
			when(repository.updateRootStatusFrom(eq(answerId), eq(QnaStatus.ACTIVE))).thenReturn(1);

			Long newId = service.createQuestion(memberId, content, answerId);

			assertThat(newId).isEqualTo(300L);

			ArgumentCaptor<QnaPost> cap = ArgumentCaptor.forClass(QnaPost.class);
			verify(repository).save(cap.capture());
			QnaPost saved = cap.getValue();

			assertAll(
				() -> assertEquals(QnaType.QUESTION, saved.getType()),
				() -> assertEquals(answerId, saved.getParentId()),
				() -> assertEquals(memberId, saved.getMemberId()),
				() -> assertEquals(content, saved.getContent()),
				() -> assertEquals(QnaStatus.ACTIVE, saved.getStatus())
			);

			verify(repository).updateRootStatusFrom(answerId, QnaStatus.ACTIVE);

			// 추가 질문도 알림 발행 없음
			verifyNoInteractions(qnaNotificationProducer);
		}

		@Test
		@DisplayName("실패: parentId가 ANSWER가 아니면 INVALID_QNA_PARENT")
		void create_followup_question_invalid_parent_type() {
			Long parentId = 777L;
			QnaPost parentQuestion = new QnaPost(
				parentId, QnaType.QUESTION, null, 1L, "부모 질문",
				QnaStatus.ACTIVE,
				LocalDateTime.now(fixedClock),
				null
			);
			when(repository.findById(parentId)).thenReturn(Optional.of(parentQuestion));

			QnaException ex = assertThrows(QnaException.class,
				() -> service.createQuestion(10L, "추가 질문", parentId));

			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_QNA_PARENT);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());

			verifyNoInteractions(qnaNotificationProducer);
		}

		@Test
		@DisplayName("실패: parentId 조회 불가 → QNA_NOT_FOUND")
		void create_followup_question_parent_not_found() {
			Long parentId = 555L;
			when(repository.findById(parentId)).thenReturn(Optional.empty());

			QnaException ex = assertThrows(QnaException.class,
				() -> service.createQuestion(10L, "추가 질문", parentId));

			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QNA_NOT_FOUND);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());

			verifyNoInteractions(qnaNotificationProducer);
		}
	}

	@Nested
	@DisplayName("createAnswer - 답변 생성")
	class CreateAnswer {

		@Test
		@DisplayName("성공: 관리자만 답변 가능, parent=QUESTION → 저장 & 루트 상태 COMPLETED + 질문작성자에게 알림 발행")
		void create_answer_success_admin() {
			Long adminId = 1L;
			Long questionId = 1000L;
			Long questionOwnerId = 30L; // 질문 작성자 (알림 수신자)
			String content = "게이트웨이가 JWT 파싱 후 X-Authorization-Id, X-Role 헤더로 전달하세요.";

			when(authorityCheckerPort.isAdmin(adminId)).thenReturn(true);

			QnaPost parentQuestion = new QnaPost(
				questionId, QnaType.QUESTION, null, questionOwnerId, "루트 질문",
				QnaStatus.ACTIVE,
				LocalDateTime.now(fixedClock),
				null
			);
			when(repository.findById(questionId)).thenReturn(Optional.of(parentQuestion));
			when(repository.save(any(QnaPost.class))).thenReturn(2000L); // answerId
			when(repository.updateRootStatusFrom(eq(questionId), eq(QnaStatus.COMPLETED))).thenReturn(1);

			Long newAnswerId = service.createAnswer(adminId, questionId, content);

			assertThat(newAnswerId).isEqualTo(2000L);

			ArgumentCaptor<QnaPost> cap = ArgumentCaptor.forClass(QnaPost.class);
			verify(repository).save(cap.capture());
			QnaPost saved = cap.getValue();

			assertAll(
				() -> assertEquals(QnaType.ANSWER, saved.getType()),
				() -> assertEquals(questionId, saved.getParentId()),
				() -> assertEquals(adminId, saved.getMemberId()),
				() -> assertEquals(content, saved.getContent()),
				() -> assertEquals(QnaStatus.ACTIVE, saved.getStatus()),
				() -> assertEquals(LocalDateTime.ofInstant(fixedClock.instant(), fixedClock.getZone()), saved.getCreatedAt())
			);

			verify(repository).updateRootStatusFrom(questionId, QnaStatus.COMPLETED);

			verify(qnaNotificationProducer).answerAdded(eq(questionOwnerId), eq(2000L));
		}

		@Test
		@DisplayName("실패: 비관리자는 NO_PERMISSION_TO_WRITE_ANSWER (알림 발행 없음)")
		void create_answer_non_admin_forbidden() {
			when(authorityCheckerPort.isAdmin(2L)).thenReturn(false);

			QnaException ex = assertThrows(QnaException.class,
				() -> service.createAnswer(2L, 10L, "비관리자 답변"));

			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NO_PERMISSION_TO_WRITE_ANSWER);
			verify(repository, never()).findById(anyLong());
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());

			verifyNoInteractions(qnaNotificationProducer);
		}

		@Test
		@DisplayName("실패: parent 질문이 존재하지 않으면 QNA_NOT_FOUND (알림 발행 없음)")
		void create_answer_parent_not_found() {
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
			when(repository.findById(999L)).thenReturn(Optional.empty());

			QnaException ex = assertThrows(QnaException.class,
				() -> service.createAnswer(1L, 999L, "답변"));

			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QNA_NOT_FOUND);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());

			verifyNoInteractions(qnaNotificationProducer);
		}

		@Test
		@DisplayName("실패: parent 타입이 QUESTION이 아니면 INVALID_QNA_PARENT (알림 발행 없음)")
		void create_answer_invalid_parent_type() {
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);

			Long parentId = 333L;
			QnaPost parent = new QnaPost(
				parentId, QnaType.ANSWER, 10L, 9L, "부모가 ANSWER",
				QnaStatus.ACTIVE,
				LocalDateTime.now(fixedClock),
				null
			);
			when(repository.findById(parentId)).thenReturn(Optional.of(parent));

			QnaException ex = assertThrows(QnaException.class,
				() -> service.createAnswer(1L, parentId, "잘못된 부모 타입"));

			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_QNA_PARENT);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());

			verifyNoInteractions(qnaNotificationProducer);
		}
	}
}