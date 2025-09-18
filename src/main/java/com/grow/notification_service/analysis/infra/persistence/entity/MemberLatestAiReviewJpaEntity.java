package com.grow.notification_service.analysis.infra.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원별 최신 AI 리뷰 기록 엔티티
 */
@Entity
@Table(name = "member_latest_ai_review")
@IdClass(MemberLatestAiReviewId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberLatestAiReviewJpaEntity {

	@Id
	@Column(nullable = false)
	private Long memberId;

	@Id
	@Column(nullable = false)
	private Long categoryId;

	@Lob
	@Column(nullable = false)
	private String quizIdsJson;

	@Column(nullable = false)
	private LocalDateTime updatedAt;
}