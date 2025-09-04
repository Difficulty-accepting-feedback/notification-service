package com.grow.notification_service.notice.application.dto;

import java.time.LocalDateTime;

import com.grow.notification_service.notice.domain.model.Notice;

/**
 * 공지사항 응답 DTO
 * 공지사항의 세부 정보를 나타내는 DTO입니다.
 * @param noticeId 공지사항 ID
 * @param title 공지사항 제목
 * @param content 공지사항 내용
 * @param pinned 공지사항 고정 여부
 * @param createdAt 공지사항 생성 일시
 */
public record NoticeResponse(
	Long noticeId,
	String title,
	String content,
	boolean pinned,
	LocalDateTime createdAt
) {
	public static NoticeResponse from(Notice n) {
		return new NoticeResponse(
			n.getNoticeId(),
			n.getTitle(),
			n.getContent(),
			n.isPinned(),
			n.getCreatedAt()
		);
	}
}