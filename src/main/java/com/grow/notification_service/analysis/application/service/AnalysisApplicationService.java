package com.grow.notification_service.analysis.application.service;

import com.grow.notification_service.analysis.domain.model.Analysis;

public interface AnalysisApplicationService {
	Analysis analyze(Long memberId, Long categoryId);
}