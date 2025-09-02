package com.grow.notification_service.qna.infra.persistence.mapper;

import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.infra.persistence.entity.QnaPostJpaEntity;

public class QnaPostMapper {

	/**
	 * 엔티티 -> 도메인
	 * @param e 엔티티
	 * @return 도메인
	 */
	public static QnaPost toDomain(QnaPostJpaEntity e) {
		return new QnaPost(
			e.getPostId(),
			e.getType(),
			e.getParentId(),
			e.getAuthorId(),
			e.getContent(),
			e.getStatus(),
			e.getCreatedAt(),
			e.getUpdatedAt()
		);
	}

	/**
	 * 도메인 -> 엔티티
	 * @param d 도메인
	 * @return 엔티티
	 */
	public static QnaPostJpaEntity toEntity(QnaPost d) {
		return QnaPostJpaEntity.builder()
			.type(d.getType())
			.parentId(d.getParentId())
			.authorId(d.getAuthorId())
			.content(d.getContent())
			.status(d.getStatus())
			.createdAt(d.getCreatedAt())
			.build();
	}
}