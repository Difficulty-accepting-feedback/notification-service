package com.grow.notification_service.notice.infra.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.grow.notification_service.notice.domain.model.Notice;
import com.grow.notification_service.notice.infra.persistence.entity.NoticeJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeMapperTest {

	@Test
	@DisplayName("NoticeJpaEntity → Notice 변환이 정상 동작한다")
	void toDomain_success() {
		// given
		LocalDateTime now = LocalDateTime.now();
		NoticeJpaEntity entity = NoticeJpaEntity.builder()
			.noticeId(1L)
			.title("공지 제목")
			.content("공지 내용")
			.isPinned(true)
			.createdAt(now)
			.build();

		// when
		Notice domain = NoticeMapper.toDomain(entity);

		// then
		assertThat(domain.getNoticeId()).isEqualTo(1L);
		assertThat(domain.getTitle()).isEqualTo("공지 제목");
		assertThat(domain.getContent()).isEqualTo("공지 내용");
		assertThat(domain.isPinned()).isTrue();
		assertThat(domain.getCreatedAt()).isEqualTo(now);
	}

	@Test
	@DisplayName("Notice → NoticeJpaEntity 변환이 정상 동작한다")
	void toEntity_success() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Notice domain = new Notice(
			2L,
			"테스트 제목",
			"테스트 내용",
			false,
			now
		);

		// when
		NoticeJpaEntity entity = NoticeMapper.toEntity(domain);

		// then
		assertThat(entity.getNoticeId()).isEqualTo(2L);
		assertThat(entity.getTitle()).isEqualTo("테스트 제목");
		assertThat(entity.getContent()).isEqualTo("테스트 내용");
		assertThat(entity.getIsPinned()).isFalse();
		assertThat(entity.getCreatedAt()).isEqualTo(now);
	}

	@Test
	@DisplayName("Entity ↔ Domain 상호 변환 시 데이터 손실 없이 유지된다")
	void roundTrip_conversion_success() {
		// given
		LocalDateTime now = LocalDateTime.of(2025, 9, 4, 12, 0);
		Notice original = new Notice(
			99L,
			"제목",
			"내용",
			true,
			now
		);

		// when
		NoticeJpaEntity entity = NoticeMapper.toEntity(original);
		Notice converted = NoticeMapper.toDomain(entity);

		// then
		assertThat(converted.getNoticeId()).isEqualTo(original.getNoticeId());
		assertThat(converted.getTitle()).isEqualTo(original.getTitle());
		assertThat(converted.getContent()).isEqualTo(original.getContent());
		assertThat(converted.isPinned()).isEqualTo(original.isPinned());
		assertThat(converted.getCreatedAt()).isEqualTo(original.getCreatedAt());
	}
}