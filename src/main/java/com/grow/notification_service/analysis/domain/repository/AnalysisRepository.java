package com.grow.notification_service.analysis.domain.repository;

import com.grow.notification_service.analysis.domain.model.Analysis;

public interface AnalysisRepository {
    Analysis save(Analysis analysis);
}