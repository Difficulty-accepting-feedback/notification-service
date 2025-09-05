package com.grow.notification_service.notice.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeTest {

	private final Clock fixedClock =
		Clock.fixed(Instant.parse("2025-09-04T10:15:30.00Z"), ZoneId.of("UTC"));

	@Test
	@DisplayName("공지 생성 시 제목, 내용, 고정 여부, 생성 시각이 올바르게 세팅된다")
	void createNotice_success() {
		// when
		Notice notice = Notice.create("제목", "내용", true, fixedClock);

		// then
		assertThat(notice.getNoticeId()).isNull(); // 생성 시점엔 ID 없음
		assertThat(notice.getTitle()).isEqualTo("제목");
		assertThat(notice.getContent()).isEqualTo("내용");
		assertThat(notice.isPinned()).isTrue();
		assertThat(notice.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 9, 4, 10, 15, 30));
	}

	@Test
	@DisplayName("공지 제목과 내용을 수정할 수 있다")
	void editNotice_success() {
		// given
		Notice notice = Notice.create("제목", "내용", false, fixedClock);

		// when
		notice.edit("수정된 제목", "수정된 내용", fixedClock);

		// then
		assertThat(notice.getTitle()).isEqualTo("수정된 제목");
		assertThat(notice.getContent()).isEqualTo("수정된 내용");
	}

	@Test
	@DisplayName("공지 고정 여부를 수정할 수 있다")
	void setPinned_success() {
		// given
		Notice notice = Notice.create("제목", "내용", false, fixedClock);

		// when
		notice.setPinned(true, fixedClock);

		// then
		assertThat(notice.isPinned()).isTrue();
	}

	@Test
	@DisplayName("공지 생성 시 제목이 null이면 예외가 발생한다")
	void createNotice_nullTitle_throwsException() {
		// when & then
		assertThatThrownBy(() -> Notice.create(null, "내용", false, fixedClock))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("제목은 필수입니다.");
	}

	@Test
	@DisplayName("공지 생성 시 내용이 null이면 예외가 발생한다")
	void createNotice_nullContent_throwsException() {
		// when & then
		assertThatThrownBy(() -> Notice.create("제목", null, false, fixedClock))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("내용은 필수입니다.");
	}
}