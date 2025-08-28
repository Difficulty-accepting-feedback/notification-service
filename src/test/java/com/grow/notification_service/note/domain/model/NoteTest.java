package com.grow.notification_service.note.domain.model;

import com.grow.notification_service.note.domain.exception.NoteDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NoteTest {

	@Nested
	@DisplayName("생성 규칙")
	class CreateRules {

		@Test
		@DisplayName("정상 생성 - 필드 초기값 확인")
		void create_success() {
			Long senderId = 1L;
			Long recipientId = 2L;
			String content = "안녕하세요";

			Note note = Note.create(senderId, recipientId, content);

			assertNull(note.getNoteId());
			assertEquals(senderId, note.getSenderId());
			assertEquals(recipientId, note.getRecipientId());
			assertEquals(content, note.getContent());
			assertNotNull(note.getCreatedAt());
			assertFalse(note.isRead());
			assertFalse(note.isSenderDeleted());
			assertFalse(note.isRecipientDeleted());
		}

		@Test
		@DisplayName("senderId가 null이면 예외")
		void create_sender_null() {
			assertThrows(NoteDomainException.class,
				() -> Note.create(null, 2L, "hi"));
		}

		@Test
		@DisplayName("recipientId가 null이면 예외")
		void create_recipient_null() {
			assertThrows(NoteDomainException.class,
				() -> Note.create(1L, null, "hi"));
		}

		@Test
		@DisplayName("자기 자신에게 전송 금지 규칙 위반 시 예외")
		void create_self_send_not_allowed() {
			assertThrows(NoteDomainException.class,
				() -> Note.create(1L, 1L, "self"));
		}

		@Test
		@DisplayName("내용 공백이면 예외")
		void create_empty_content() {
			assertThrows(NoteDomainException.class,
				() -> Note.create(1L, 2L, "  "));
		}
	}

	@Nested
	@DisplayName("읽음 처리 규칙")
	class ReadRules {

		@Test
		@DisplayName("수신자가 읽음 처리하면 isRead=true")
		void markRead_by_recipient() {
			Note note = Note.create(1L, 2L, "msg");
			note.markRead(2L);
			assertTrue(note.isRead());
		}

		@Test
		@DisplayName("수신자가 아닌 사용자가 읽음 처리하면 예외")
		void markRead_by_non_recipient() {
			Note note = Note.create(1L, 2L, "msg");
			assertThrows(NoteDomainException.class, () -> note.markRead(3L));
		}
	}

	@Nested
	@DisplayName("삭제 규칙 (소프트/물리 삭제 판정)")
	class DeleteRules {

		@Test
		@DisplayName("발신자가 삭제하면 senderDeleted=true")
		void delete_by_sender() {
			Note note = Note.create(1L, 2L, "msg");
			note.deleteBy(1L);
			assertTrue(note.isSenderDeleted());
			assertFalse(note.isRecipientDeleted());
			assertFalse(note.deletablePhysically());
		}

		@Test
		@DisplayName("수신자가 삭제하면 recipientDeleted=true")
		void delete_by_recipient() {
			Note note = Note.create(1L, 2L, "msg");
			note.deleteBy(2L);
			assertTrue(note.isRecipientDeleted());
			assertFalse(note.isSenderDeleted());
			assertFalse(note.deletablePhysically());
		}

		@Test
		@DisplayName("발신자와 수신자 모두 삭제하면 물리 삭제 대상")
		void deletable_physically_when_both_deleted() {
			Note note = Note.create(1L, 2L, "msg");
			note.deleteBy(1L); // sender
			note.deleteBy(2L); // recipient
			assertTrue(note.isSenderDeleted());
			assertTrue(note.isRecipientDeleted());
			assertTrue(note.deletablePhysically());
		}

		@Test
		@DisplayName("제3자가 삭제 시도하면 예외")
		void delete_by_third_party() {
			Note note = Note.create(1L, 2L, "msg");
			assertThrows(NoteDomainException.class, () -> note.deleteBy(999L));
		}
	}

	@Nested
	@DisplayName("복원 생성자 동작")
	class RestoreConstructor {

		@Test
		@DisplayName("null 플래그는 false로 해석된다")
		void null_flags_become_false() {
			LocalDateTime now = LocalDateTime.now();
			Note restored = new Note(
				10L, 1L, 2L, "restored",
				now, null, null, null
			);

			assertEquals(10L, restored.getNoteId());
			assertEquals(1L, restored.getSenderId());
			assertEquals(2L, restored.getRecipientId());
			assertEquals("restored", restored.getContent());
			assertEquals(now, restored.getCreatedAt());
			assertFalse(restored.isRead());
			assertFalse(restored.isSenderDeleted());
			assertFalse(restored.isRecipientDeleted());
		}
	}
}