package com.grow.notification_service.qna.infra.persistence.entity;

import java.time.LocalDateTime;

import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(
	name = "qna_post",
	indexes = {
		// parentId 단일: CTE 상향 탐색 최적화 (JOIN up u ON p.post_id = u.parent_id)
		@Index(name = "idx_qna_parent", columnList = "parent_id"),
		// parentId + createdAt: 트리 조회/정렬 최적화
		@Index(name = "idx_qna_parent_created", columnList = "parent_id, created_at"),
		// type + parentId: 질문/답변 필터 시
		@Index(name = "idx_qna_type_parent", columnList = "type, parent_id"),
		// type + parentId + createdAt: 복합 정렬/필터
		@Index(name = "idx_qna_type_parent_created", columnList = "type, parent_id, created_at"),
		// memberId + createdAt: 내 질문 목록
		@Index(name = "idx_qna_member_created", columnList = "member_id, created_at")
	}
)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaPostJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "post_id")
	private Long postId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private QnaType type; // QUESTION/ANSWER

	@Column(name = "parent_id")
	private Long parentId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private QnaStatus status; // ACTIVE/DELETED

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) createdAt = LocalDateTime.now();
		if (status == null) status = QnaStatus.ACTIVE;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}