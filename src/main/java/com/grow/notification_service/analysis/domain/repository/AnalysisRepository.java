package com.grow.ai_coaching_service.analysis.domain.repository;

import com.grow.ai_coaching_service.analysis.domain.model.Analysis;

public interface AnalysisRepository {
    Analysis save(Analysis analysis);
}
