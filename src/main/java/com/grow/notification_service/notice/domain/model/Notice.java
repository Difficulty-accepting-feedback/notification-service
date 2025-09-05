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
	private LocalDateTime updatedAt; // 수정 시각

	public Notice(Long noticeId,
		String title,
		String content,
		boolean pinned,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
		this.noticeId = noticeId;
		this.title = Objects.requireNonNull(title, "제목은 필수입니다.");
		this.content = Objects.requireNonNull(content, "내용은 필수입니다.");
		this.pinned = pinned;
		this.createdAt = Objects.requireNonNull(createdAt, "생성시각은 필수입니다.");
		this.updatedAt = updatedAt;
	}

	/** 공지 생성 */
	public static Notice create(String title, String content, boolean pinned, Clock clock) {
		LocalDateTime now = LocalDateTime.now(clock);
		return new Notice(null, title, content, pinned, now, now);
	}

	/** 공지 내용 수정: 값이 달라질 때만 업데이트 */
	public boolean edit(String title, String content, Clock clock) {
		boolean changed = false;
		if (!Objects.equals(this.title, title)) {
			this.title = Objects.requireNonNull(title, "제목은 필수입니다.");
			changed = true;
		}
		if (!Objects.equals(this.content, content)) {
			this.content = Objects.requireNonNull(content, "내용은 필수입니다.");
			changed = true;
		}
		if (changed) {
			this.updatedAt = LocalDateTime.now(clock);
		}
		return changed;
	}

	/** 고정 상태 설정: 값이 달라질 때만 업데이트 */
	public boolean setPinned(boolean pinned, Clock clock) {
		if (this.pinned == pinned) {
			return false;
		}
		this.pinned = pinned;
		this.updatedAt = LocalDateTime.now(clock);
		return true;
	}
}