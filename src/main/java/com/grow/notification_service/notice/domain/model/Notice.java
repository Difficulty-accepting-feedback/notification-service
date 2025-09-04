package com.grow.notification_service.notice.domain.model;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;

@Getter
public class Notice {

	private final Long noticeId;
	private String title;
	private String content;
	private boolean pinned;                 // 상단 고정 여부
	private final LocalDateTime createdAt;  // 생성 시각

	public Notice(Long noticeId,
		String title,
		String content,
		boolean pinned,
		LocalDateTime createdAt) {
		this.noticeId = noticeId;
		this.title = Objects.requireNonNull(title, "제목은 필수입니다.");
		this.content = Objects.requireNonNull(content, "내용은 필수입니다.");
		this.pinned = pinned;
		this.createdAt = Objects.requireNonNull(createdAt, "생성시각은 필수입니다.");
	}

	/** 공지 생성 */
	public static Notice create(String title, String content, boolean pinned, Clock clock) {
		return new Notice(null, title, content, pinned, LocalDateTime.now(clock));
	}

	/** 공지 내용 수정  */
	public void edit(String title, String content) {
		this.title = Objects.requireNonNull(title, "제목은 필수입니다.");
		this.content = Objects.requireNonNull(content, "내용은 필수입니다.");
	}

	/** 고정 상태 설정  */
	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}
}