package com.grow.notification_service.analysis.domain.repository;

import java.util.Optional;

import com.grow.notification_service.analysis.domain.model.Analysis;

public interface AnalysisRepository {
    Analysis save(Analysis analysis);
    Optional<Analysis> findTopByCategoryIdAndSessionIdOrderByAnalysisIdDesc(Long categoryId, String sessionId);
}