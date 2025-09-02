package com.grow.ai_coaching_service.analysis.infra.persistence.repository;

import com.grow.ai_coaching_service.analysis.infra.persistence.entity.AnalysisJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJpaRepository extends JpaRepository<AnalysisJpaEntity,Long> {
}
