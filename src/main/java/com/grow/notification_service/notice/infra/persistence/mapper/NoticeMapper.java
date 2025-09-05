package com.grow.notification_service.notice.infra.persistence.mapper;

import com.grow.notification_service.notice.domain.model.Notice;
import com.grow.notification_service.notice.infra.persistence.entity.NoticeJpaEntity;

public class NoticeMapper {

	public static Notice toDomain(NoticeJpaEntity e) {
		return new Notice(
			e.getNoticeId(),
			e.getTitle(),
			e.getContent(),
			e.getIsPinned(),
			e.getCreatedAt(),
			e.getUpdatedAt()
		);
	}

	public static NoticeJpaEntity toEntity(Notice n) {
		return NoticeJpaEntity.builder()
			.noticeId(n.getNoticeId())
			.title(n.getTitle())
			.content(n.getContent())
			.isPinned(n.isPinned())
			.createdAt(n.getCreatedAt())
			.updatedAt(n.getUpdatedAt())
			.build();
	}
}