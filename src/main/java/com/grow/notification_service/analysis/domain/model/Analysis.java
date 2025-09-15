package com.grow.notification_service.analysis.domain.model;

import lombok.Getter;

@Getter
public class Analysis {
    private final Long analysisId;
    private final Long memberId;
    private final Long categoryId;
    private final String analysisResult;

    public Analysis(Long memberId, Long categoryId, String analysisResult) {
        this(null, memberId, categoryId, analysisResult);
    }
    public Analysis(
        Long analysisId,
        Long memberId,
        Long categoryId,
        String analysisResult
    ) {
        this.analysisId = analysisId;
        this.memberId = memberId;
        this.categoryId = categoryId;
        this.analysisResult = analysisResult;
    }
}