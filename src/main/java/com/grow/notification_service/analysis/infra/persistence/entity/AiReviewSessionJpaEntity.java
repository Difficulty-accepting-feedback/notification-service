package com.grow.notification_service.analysis.infra.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_review_session",
	indexes = {
		@Index(name="idx_ars_member_date", columnList = "memberId,createdAt"),
		@Index(name="idx_ars_category_date", columnList = "categoryId,createdAt")
	})
public class AiReviewSessionJpaEntity {

	@Id
	@Column(length = 64)
	private String sessionId;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false)
	private Long categoryId;

	@Lob
	@Column(nullable = false)
	private String quizIdsJson;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column
	private Long analysisId;
}