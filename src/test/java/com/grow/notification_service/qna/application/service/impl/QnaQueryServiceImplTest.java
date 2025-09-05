package com.grow.notification_service.qna.application.service.impl;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.QnaException;
import com.grow.notification_service.qna.application.dto.QnaThreadNode;
import com.grow.notification_service.qna.application.dto.QnaThreadResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[App][QnA] QnaQueryServiceImpl 커버리지 테스트 (GROW MSA/DDD)")
class QnaQueryServiceImplTest {

	@Mock
	QnaPostRepository repository;

	@Mock
	AuthorityCheckerPort authorityCheckerPort;

	QnaQueryServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new QnaQueryServiceImpl(repository, authorityCheckerPort);
	}

	// 샘플 헬퍼
	private static QnaPost q(long id, Long parentId, long memberId, String content, LocalDateTime at) {
		return new QnaPost(id, QnaType.QUESTION, parentId, memberId, content, QnaStatus.ACTIVE, at, null);
	}
	private static QnaPost a(long id, Long parentId, long memberId, String content, LocalDateTime at) {
		return new QnaPost(id, QnaType.ANSWER, parentId, memberId, content, QnaStatus.ACTIVE, at, null);
	}

	// ============ 스레드 조회(관리자) ============
	@Nested
	@DisplayName("getThreadAsAdmin")
	class GetThreadAsAdmin {

		@Test
		@DisplayName("관리자 아님 → QNA_FORBIDDEN")
		void notAdmin_forbidden() {
			when(authorityCheckerPort.isAdmin(9L)).thenReturn(false);
			QnaException ex = assertThrows(QnaException.class, () -> service.getThreadAsAdmin(1L, 9L));
			assertEquals(ErrorCode.QNA_FORBIDDEN, ex.getErrorCode());
			verify(repository, never()).findTreeFlat(anyLong());
		}

		@Test
		@DisplayName("flat 비어있음 → QNA_NOT_FOUND")
		void emptyFlat_notFound() {
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
			when(repository.findTreeFlat(99L)).thenReturn(List.of());
			QnaException ex = assertThrows(QnaException.class, () -> service.getThreadAsAdmin(99L, 1L));
			assertEquals(ErrorCode.QNA_NOT_FOUND, ex.getErrorCode());
		}

		@Test
		@DisplayName("flat[0]이 루트/QUESTION 아님 → INVALID_QNA_PARENT")
		void invalidRoot_guard() {
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
			LocalDateTime t0 = LocalDateTime.of(2025, 9, 4, 10, 0);
			when(repository.findTreeFlat(1L)).thenReturn(List.of(
				a(10L, 1L, 2L, "잘못된 루트", t0),
				q(1L, null, 100L, "원래 루트", t0.plusMinutes(1))
			));
			QnaException ex = assertThrows(QnaException.class, () -> service.getThreadAsAdmin(1L, 1L));
			assertEquals(ErrorCode.INVALID_QNA_PARENT, ex.getErrorCode());
		}

		@Test
		@DisplayName("성공: flat→트리 링크 & createdAt 오름차순 정렬 재귀 적용")
		void success_buildTree_and_sort() {
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
			LocalDateTime t0 = LocalDateTime.of(2025, 9, 4, 10, 0);
			LocalDateTime t1 = t0.plusMinutes(1);
			LocalDateTime t2 = t0.plusMinutes(2);

			// root(Q1)-A2-Q3
			when(repository.findTreeFlat(1L)).thenReturn(List.of(
				q(1L, null, 100L, "루트 질문", t0),
				a(2L, 1L, 999L, "관리자 답변", t1),
				q(3L, 2L, 100L, "꼬리 질문", t2)
			));

			QnaThreadResponse resp = service.getThreadAsAdmin(1L, 1L);

			QnaThreadNode root = resp.root();
			assertEquals(1L, root.getId());
			assertEquals(1, root.getChildren().size());
			QnaThreadNode a2 = root.getChildren().get(0);
			assertEquals(2L, a2.getId());
			assertEquals(1, a2.getChildren().size());
			assertEquals(3L, a2.getChildren().get(0).getId());

			// 정렬 검증 (오름차순)
			assertTrue(root.getChildren().get(0).getCreatedAt().isAfter(root.getCreatedAt()));
			assertTrue(a2.getChildren().get(0).getCreatedAt().isAfter(a2.getCreatedAt()));

			verify(repository).findTreeFlat(1L);
		}
	}

	// ============ 스레드 조회(개인) ============
	@Nested
	@DisplayName("getMyThread")
	class GetMyThread {

		@Test
		@DisplayName("viewerId null → QNA_FORBIDDEN")
		void viewerNull_forbidden() {
			QnaException ex = assertThrows(QnaException.class, () -> service.getMyThread(1L, null));
			assertEquals(ErrorCode.QNA_FORBIDDEN, ex.getErrorCode());
			verify(repository, never()).findTreeFlat(anyLong());
		}

		@Test
		@DisplayName("소유자 불일치 → QNA_FORBIDDEN")
		void ownerMismatch_forbidden() {
			LocalDateTime t0 = LocalDateTime.of(2025, 9, 4, 10, 0);
			when(repository.findTreeFlat(1L)).thenReturn(List.of(
				q(1L, null, 100L, "내 질문 아님", t0)
			));
			QnaException ex = assertThrows(QnaException.class, () -> service.getMyThread(1L, 200L));
			assertEquals(ErrorCode.QNA_FORBIDDEN, ex.getErrorCode());
		}

		@Test
		@DisplayName("성공: 소유자 일치 시 트리 반환")
		void success_ownerMatches() {
			LocalDateTime t0 = LocalDateTime.of(2025, 9, 4, 10, 0);
			when(repository.findTreeFlat(1L)).thenReturn(List.of(
				q(1L, null, 100L, "내 질문", t0),
				a(2L, 1L, 999L, "답변", t0.plusMinutes(1)),
				q(3L, 2L, 100L, "꼬리 질문", t0.plusMinutes(2))
			));

			QnaThreadResponse resp = service.getMyThread(1L, 100L);

			assertEquals(1L, resp.root().getId());
			assertEquals(1, resp.root().getChildren().size());
			assertEquals(2L, resp.root().getChildren().get(0).getId());
			assertEquals(1, resp.root().getChildren().get(0).getChildren().size());
			assertEquals(3L, resp.root().getChildren().get(0).getChildren().get(0).getId());
		}
	}

	// ============ 루트 목록 조회 ============
	@Nested
	@DisplayName("루트 질문 목록")
	class RootList {

		@Test
		@DisplayName("관리자 목록: 관리자 아님 → QNA_FORBIDDEN")
		void adminList_forbidden() {
			when(authorityCheckerPort.isAdmin(5L)).thenReturn(false);
			QnaException ex = assertThrows(QnaException.class,
				() -> service.getRootQuestionsAsAdmin(PageRequest.of(0, 10), 5L));
			assertEquals(ErrorCode.QNA_FORBIDDEN, ex.getErrorCode());
			verify(repository, never()).findRootQuestions(any());
		}

		@Test
		@DisplayName("관리자 목록: 정상 반환")
		void adminList_success() {
			when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
			LocalDateTime t0 = LocalDateTime.of(2025, 9, 4, 10, 0);
			Page<QnaPost> page = new PageImpl<>(
				List.of(q(1L, null, 10L, "Q1", t0), q(2L, null, 11L, "Q2", t0.plusMinutes(1))),
				PageRequest.of(0, 2), 5
			);
			when(repository.findRootQuestions(any())).thenReturn(page);

			Page<QnaPost> result = service.getRootQuestionsAsAdmin(PageRequest.of(0, 2), 1L);

			assertEquals(2, result.getContent().size());
			assertEquals(5, result.getTotalElements());
			verify(repository).findRootQuestions(any());
		}

		@Test
		@DisplayName("개인 목록: viewerId null → QNA_FORBIDDEN")
		void myList_forbidden_viewerNull() {
			QnaException ex = assertThrows(QnaException.class,
				() -> service.getMyRootQuestions(PageRequest.of(0, 10), null));
			assertEquals(ErrorCode.QNA_FORBIDDEN, ex.getErrorCode());
			verify(repository, never()).findMyRootQuestions(anyLong(), any());
		}

		@Test
		@DisplayName("개인 목록: 정상 반환 (본인 소유)")
		void myList_success() {
			LocalDateTime t0 = LocalDateTime.of(2025, 9, 4, 10, 0);
			Page<QnaPost> page = new PageImpl<>(
				List.of(q(10L, null, 100L, "내 질문1", t0)),
				PageRequest.of(0, 10), 1
			);
			when(repository.findMyRootQuestions(eq(100L), any())).thenReturn(page);

			Page<QnaPost> result = service.getMyRootQuestions(PageRequest.of(0, 10), 100L);

			assertEquals(1, result.getTotalElements());
			assertEquals(1, result.getContent().size());
			verify(repository).findMyRootQuestions(eq(100L), any());
		}
	}
}