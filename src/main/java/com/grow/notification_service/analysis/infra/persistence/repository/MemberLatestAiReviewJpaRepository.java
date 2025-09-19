package com.grow.notification_service.analysis.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.analysis.infra.persistence.entity.MemberLatestAiReviewId;
import com.grow.notification_service.analysis.infra.persistence.entity.MemberLatestAiReviewJpaEntity;

public interface MemberLatestAiReviewJpaRepository
	extends JpaRepository<MemberLatestAiReviewJpaEntity, MemberLatestAiReviewId> {}