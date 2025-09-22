package com.grow.notification_service.analysis.infra.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.analysis.infra.persistence.entity.AnalysisJpaEntity;

public interface AnalysisJpaRepository extends JpaRepository<AnalysisJpaEntity,Long> {
	Optional<AnalysisJpaEntity> findTopByCategoryIdAndSessionIdOrderByAnalysisIdDesc(Long categoryId, String sessionId);
}