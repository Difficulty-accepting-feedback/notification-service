package com.grow.notification_service.note.infra.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.infra.persistence.entity.NoteJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoteMapperTest {

	private final NoteMapper mapper = new NoteMapper();

	private NoteJpaEntity sampleEntity() {
		LocalDateTime now = LocalDateTime.of(2025, 9, 4, 12, 0);
		return NoteJpaEntity.builder()
			.noteId(101L)
			.senderId(1L)
			.recipientId(2L)
			.senderNickname("보낸이")
			.recipientNickname("받는이")
			.content("메시지 내용")
			.createdAt(now)
			.isRead(false)
			.senderDeleted(false)
			.recipientDeleted(true)
			.build();
	}

	private Note sampleDomain() {
		LocalDateTime now = LocalDateTime.of(2025, 9, 4, 12, 0);
		return new Note(
			202L,           // noteId
			10L,            // senderId
			20L,            // recipientId
			"도메인 메시지",   // content
			now,            // createdAt
			true,           // isRead
			true,           // senderDeleted
			false,          // recipientDeleted
			"도메인보낸이",     // senderNickname
			"도메인받는이"      // recipientNickname
		);
	}

	@Test
	@DisplayName("Entity → Domain 매핑이 모든 필드를 정확히 전달한다")
	void toDomain_success() {
		// given
		NoteJpaEntity e = sampleEntity();

		// when
		Note d = mapper.toDomain(e);

		// then
		assertThat(d.getNoteId()).isEqualTo(101L);
		assertThat(d.getSenderId()).isEqualTo(1L);
		assertThat(d.getRecipientId()).isEqualTo(2L);
		assertThat(d.getSenderNickname()).isEqualTo("보낸이");
		assertThat(d.getRecipientNickname()).isEqualTo("받는이");
		assertThat(d.getContent()).isEqualTo("메시지 내용");
		assertThat(d.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 9, 4, 12, 0));
		assertThat(d.isRead()).isFalse();
		assertThat(d.isSenderDeleted()).isFalse();
		assertThat(d.isRecipientDeleted()).isTrue();
	}

	@Test
	@DisplayName("Domain → Entity 매핑이 모든 필드를 정확히 전달한다")
	void toEntity_success() {
		// given
		Note d = sampleDomain();

		// when
		NoteJpaEntity e = mapper.toEntity(d);

		// then
		assertThat(e.getNoteId()).isEqualTo(202L);
		assertThat(e.getSenderId()).isEqualTo(10L);
		assertThat(e.getRecipientId()).isEqualTo(20L);
		assertThat(e.getSenderNickname()).isEqualTo("도메인보낸이");
		assertThat(e.getRecipientNickname()).isEqualTo("도메인받는이");
		assertThat(e.getContent()).isEqualTo("도메인 메시지");
		assertThat(e.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 9, 4, 12, 0));
		assertThat(e.getIsRead()).isTrue();
		assertThat(e.getSenderDeleted()).isTrue();
		assertThat(e.getRecipientDeleted()).isFalse();
	}

	@Test
	@DisplayName("Entity → Domain → Entity 라운드트립 시 데이터 손실이 없다")
	void roundTrip_entityToDomainToEntity() {
		// given
		NoteJpaEntity origin = sampleEntity();

		// when
		Note d = mapper.toDomain(origin);
		NoteJpaEntity back = mapper.toEntity(d);

		// then
		assertThat(back.getNoteId()).isEqualTo(origin.getNoteId());
		assertThat(back.getSenderId()).isEqualTo(origin.getSenderId());
		assertThat(back.getRecipientId()).isEqualTo(origin.getRecipientId());
		assertThat(back.getSenderNickname()).isEqualTo(origin.getSenderNickname());
		assertThat(back.getRecipientNickname()).isEqualTo(origin.getRecipientNickname());
		assertThat(back.getContent()).isEqualTo(origin.getContent());
		assertThat(back.getCreatedAt()).isEqualTo(origin.getCreatedAt());
		assertThat(back.getIsRead()).isEqualTo(origin.getIsRead());
		assertThat(back.getSenderDeleted()).isEqualTo(origin.getSenderDeleted());
		assertThat(back.getRecipientDeleted()).isEqualTo(origin.getRecipientDeleted());
	}

	@Test
	@DisplayName("null 인자를 넘기면 NPE가 발생한다(현재 구현의 계약)")
	void nullArgument_throwsNpe() {
		assertThatThrownBy(() -> mapper.toDomain(null))
			.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> mapper.toEntity(null))
			.isInstanceOf(NullPointerException.class);
	}
}