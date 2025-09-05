package com.grow.notification_service.qna.application.service.impl;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QnaException;
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

	Clock fixedClock;

	QnaCommandServiceImpl service;

	@BeforeEach
	void setUp() {
		fixedClock = Clock.fixed(Instant.parse("2025-09-04T12:00:00Z"), ZoneId.of("UTC"));
		service = new QnaCommandServiceImpl(repository, authorityCheckerPort, fixedClock);
	}

	@Nested
	@DisplayName("createQuestion - 질문 생성")
	class CreateQuestion {

		@Test
		@DisplayName("성공: 루트 질문 생성 (parentId=null) → 저장 & 상태 업데이트 없음")
		void create_root_question_success() {
			// given
			Long memberId = 10L;
			String content = "게이트웨이에서 권한을 어떻게 전달하나요?";
			when(repository.save(any(QnaPost.class))).thenReturn(100L);

			// when
			Long postId = service.createQuestion(memberId, content, null);

			// then
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
		}

		@Test
		@DisplayName("성공: 추가 질문 생성 (parent=ANSWER) → 루트 상태 ACTIVE로 토글")
		void create_followup_question_success() {
			// given
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

			// when
			Long newId = service.createQuestion(memberId, content, answerId);

			// then
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
		}

		@Test
		@DisplayName("실패: parentId가 ANSWER가 아니면 INVALID_QNA_PARENT")
		void create_followup_question_invalid_parent_type() {
			// given
			Long parentId = 777L;
			QnaPost parentQuestion = new QnaPost(
				parentId, QnaType.QUESTION, null, 1L, "부모 질문",
				QnaStatus.ACTIVE,
				LocalDateTime.now(fixedClock),
				null
			);
			when(repository.findById(parentId)).thenReturn(Optional.of(parentQuestion));

			// when
			QnaException ex = assertThrows(QnaException.class,
				() -> service.createQuestion(10L, "추가 질문", parentId));

			// then
			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_QNA_PARENT);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());
		}

		@Test
		@DisplayName("실패: parentId 조회 불가 → QNA_NOT_FOUND")
		void create_followup_question_parent_not_found() {
			// given
			Long parentId = 555L;
			when(repository.findById(parentId)).thenReturn(Optional.empty());

			// when
			QnaException ex = assertThrows(QnaException.class,
				() -> service.createQuestion(10L, "추가 질문", parentId));

			// then
			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QNA_NOT_FOUND);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());
		}
	}

	@Nested
	@DisplayName("createAnswer - 답변 생성")
	class CreateAnswer {

		@Test
		@DisplayName("성공: 관리자만 답변 가능, parent=QUESTION → 저장 & 루트 상태 COMPLETED")
		void create_answer_success_admin() {
			// given
			Long adminId = 1L;
			Long questionId = 1000L;
			String content = "게이트웨이가 JWT 파싱 후 X-Authorization-Id, X-Role 헤더로 전달하세요.";

			when(authorityCheckerPort.isAdmin(adminId)).thenReturn(true);

			QnaPost parentQuestion = new QnaPost(
				questionId, QnaType.QUESTION, null, 30L, "루트 질문",
				QnaStatus.ACTIVE,
				LocalDateTime.now(fixedClock),
				null
			);
			when(repository.findById(questionId)).thenReturn(Optional.of(parentQuestion));
			when(repository.save(any(QnaPost.class))).thenReturn(2000L);
			when(repository.updateRootStatusFrom(eq(questionId), eq(QnaStatus.COMPLETED))).thenReturn(1);

			// when
			Long newAnswerId = service.createAnswer(adminId, questionId, content);

			// then
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
		}

		@Test
		@DisplayName("실패: 비관리자는 NO_PERMISSION_TO_WRITE_ANSWER")
		void create_answer_non_admin_forbidden() {
			// given
			when(authorityCheckerPort.isAdmin(2L)).thenReturn(false);

			// when
			QnaException ex = assertThrows(QnaException.class,
				() -> service.createAnswer(2L, 10L, "비관리자 답변"));

			// then
			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NO_PERMISSION_TO_WRITE_ANSWER);
			verify(repository, never()).findById(anyLong());
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());
		}

		@Test
		@DisplayName("실패: parent 질문이 존재하지 않으면 QNA_NOT_FOUND")
		void create_answer_parent_not_found() {
			// given
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
			when(repository.findById(999L)).thenReturn(Optional.empty());

			// when
			QnaException ex = assertThrows(QnaException.class,
				() -> service.createAnswer(1L, 999L, "답변"));

			// then
			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QNA_NOT_FOUND);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());
		}

		@Test
		@DisplayName("실패: parent 타입이 QUESTION이 아니면 INVALID_QNA_PARENT")
		void create_answer_invalid_parent_type() {
			// given
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);

			Long parentId = 333L;
			QnaPost parent = new QnaPost(
				parentId, QnaType.ANSWER, 10L, 9L, "부모가 ANSWER",
				QnaStatus.ACTIVE,
				LocalDateTime.now(fixedClock),
				null
			);
			when(repository.findById(parentId)).thenReturn(Optional.of(parent));

			// when
			QnaException ex = assertThrows(QnaException.class,
				() -> service.createAnswer(1L, parentId, "잘못된 부모 타입"));

			// then
			assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_QNA_PARENT);
			verify(repository, never()).save(any());
			verify(repository, never()).updateRootStatusFrom(anyLong(), any());
		}
	}
}