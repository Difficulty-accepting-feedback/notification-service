package com.grow.notification_service.analysis.infra.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.analysis.infra.persistence.entity.AiReviewSessionJpaEntity;

public interface AiReviewSessionJpaRepository extends JpaRepository<AiReviewSessionJpaEntity, String> {

	List<AiReviewSessionJpaEntity> findByMemberIdAndCategoryIdAndCreatedAtBetweenOrderByCreatedAtDesc(
		Long memberId, Long categoryId, LocalDateTime from, LocalDateTime to);

	List<AiReviewSessionJpaEntity> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
		Long memberId, LocalDateTime from, LocalDateTime to);
}