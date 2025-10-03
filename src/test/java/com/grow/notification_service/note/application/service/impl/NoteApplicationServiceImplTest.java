package com.grow.notification_service.note.application.service.impl;

import com.grow.notification_service.common.exception.DomainException;
import com.grow.notification_service.global.metrics.NotificationMetrics;
import com.grow.notification_service.note.application.dto.NotePageResponse;
import com.grow.notification_service.note.application.dto.NoteResponse;
import com.grow.notification_service.note.application.event.NoteNotificationProducer;
import com.grow.notification_service.note.application.port.MemberPort;
import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.domain.repository.NoteRepository;
import com.grow.notification_service.note.presentation.dto.SendNoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteApplicationServiceImplTest {

	@Mock
	private NoteRepository noteRepository;

	@Mock
	private MemberPort memberPort;

	@Mock
	private NoteNotificationProducer noteNotificationProducer;

	@Mock
	private NotificationMetrics metrics;

	@InjectMocks
	private com.grow.notification_service.note.application.service.impl.NoteApplicationServiceImpl service;

	@BeforeEach
	void setUp() {
		lenient().doNothing().when(metrics).result(anyString(), any(String[].class));
		lenient().doNothing().when(noteNotificationProducer).noteReceived(anyLong(), anyLong());
	}

	@Nested
	@DisplayName("쪽지 전송")
	class SendTests {

		@Test
		@DisplayName("정상 전송 시 NoteResponse 반환 (닉네임 → memberId 해석) + 수신 알림 이벤트 발행 1건")
		void send_success() {
			Long senderId = 1L;
			String recipientNickname = "상대닉";
			Long recipientId = 2L;
			SendNoteRequest req = new SendNoteRequest(recipientNickname, "안녕!");

			// 멤버 포트: 닉네임 해석 + 발신자 닉네임 조회
			given(memberPort.resolveByNickname(recipientNickname))
				.willReturn(new MemberPort.ResolveResult(recipientId, recipientNickname));
			given(memberPort.getMemberName(senderId)).willReturn("보낸이닉");

			// 저장 후 반환될 도메인 객체(영속 상태 가정)
			LocalDateTime fixed = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
			Note saved = new Note(
				10L, senderId, recipientId, "안녕!",
				fixed, false, false, false, "보낸이닉", "상대닉"
			);
			given(noteRepository.save(any(Note.class))).willReturn(saved);

			NoteResponse response = service.send(senderId, req);

			assertNotNull(response);
			assertEquals(10L, response.noteId());
			assertEquals(senderId, response.senderId());
			assertEquals(recipientId, response.recipientId());
			assertEquals("안녕!", response.content());
			assertFalse(response.isRead());
			assertEquals(fixed, response.createdAt());

			verify(memberPort).resolveByNickname(recipientNickname);
			verify(memberPort).getMemberName(senderId);
			verify(noteRepository).save(any(Note.class));

			verify(noteNotificationProducer, times(1)).noteReceived(eq(recipientId), eq(10L));
		}

		@Test
		@DisplayName("자기 자신에게는 전송 불가 (도메인 규칙 위반)")
		void send_self_not_allowed() {
			Long senderId = 1L;
			String recipientNickname = "나";
			SendNoteRequest req = new SendNoteRequest(recipientNickname, "나에게...");

			// 닉네임 해석 결과가 본인
			given(memberPort.resolveByNickname(recipientNickname))
				.willReturn(new MemberPort.ResolveResult(senderId, recipientNickname));
			given(memberPort.getMemberName(senderId)).willReturn("나");

			assertThrows(DomainException.class, () -> service.send(senderId, req));

			// 실패 경로에서 저장/이벤트가 실행되지 않았는지 간단 체크(선택)
			verify(noteRepository, never()).save(any(Note.class));
			verify(noteNotificationProducer, never()).noteReceived(anyLong(), anyLong());
		}
	}

	@Nested
	@DisplayName("보관함 조회")
	class BoxQueryTests {

		@Test
		@DisplayName("수신함 페이지 조회 - 소프트 삭제 제외, 최신순")
		void inbox_success() {
			Long memberId = 2L;
			int page = 0;
			int size = 2;

			Note n1 = new Note(11L, 1L, memberId, "첫 번째", LocalDateTime.now().minusMinutes(1), false, false, false, "보낸이1", "나");
			Note n2 = new Note(12L, 3L, memberId, "두 번째", LocalDateTime.now().minusMinutes(2), true, false, false, "보낸이3", "나");
			Page<Note> notePage = new PageImpl<>(Arrays.asList(n1, n2), PageRequest.of(page, size), 2);

			given(noteRepository.findReceived(eq(memberId), eq(PageRequest.of(page, size)))).willReturn(notePage);

			NotePageResponse res = service.inbox(memberId, page, size);

			assertNotNull(res);
			assertEquals(0, res.page());
			assertEquals(2, res.size());
			assertEquals(2L, res.totalElements());
			assertEquals(1, res.totalPages());
			assertEquals(2, res.content().size());
		}

		@Test
		@DisplayName("발신함 페이지 조회 - 소프트 삭제 제외, 최신순")
		void outbox_success() {
			Long memberId = 1L;
			int page = 0;
			int size = 1;

			Note n1 = new Note(13L, memberId, 2L, "보낸 메시지", LocalDateTime.now(), false, false, false, "나", "상대닉");
			Page<Note> notePage = new PageImpl<>(Collections.singletonList(n1), PageRequest.of(page, size), 1);

			given(noteRepository.findSent(eq(memberId), eq(PageRequest.of(page, size)))).willReturn(notePage);

			NotePageResponse res = service.outbox(memberId, page, size);

			assertNotNull(res);
			assertEquals(1, res.totalElements());
			assertEquals(1, res.content().size());
			assertEquals(13L, res.content().get(0).noteId());
		}
	}

	@Nested
	@DisplayName("읽음/삭제/카운트")
	class MutateAndCountTests {

		@Test
		@DisplayName("수신자가 읽음 처리하면 리포지토리 호출됨")
		void markRead_success() {
			Long memberId = 2L;
			Long noteId = 99L;

			service.markRead(memberId, noteId);

			verify(noteRepository).markRead(noteId, memberId);
		}

		@Test
		@DisplayName("삭제 시 소프트 삭제 후, 양측 삭제 상태면 물리 삭제 시도")
		void delete_flow_success() {
			Long memberId = 2L;
			Long noteId = 100L;

			service.delete(memberId, noteId);

			InOrder inOrder = inOrder(noteRepository);
			inOrder.verify(noteRepository).softDelete(noteId, memberId);
			inOrder.verify(noteRepository).deletePhysicallyIfBothDeleted(noteId);
		}

		@Test
		@DisplayName("미읽음 개수 조회")
		void unreadCount_success() {
			Long memberId = 2L;
			given(noteRepository.countUnread(memberId)).willReturn(3L);

			long count = service.unreadCount(memberId);

			assertEquals(3L, count);
			verify(noteRepository).countUnread(memberId);
		}
	}
}