package com.grow.ai_coaching_service.analysis.domain.model;

import lombok.Getter;

@Getter
public class Analysis {

    private Long analysisId;
    private Long memberId; // 멤버 ID
    private Long categoryId; // 카테고리 ID
    private String analysisResult; // 분석 결과

    public Analysis(Long memberId,
                    Long categoryId,
                    String analysisResult
    ) {
        this.analysisId = null;
        this.memberId = memberId;
        this.categoryId = categoryId;
        this.analysisResult = analysisResult;
    }
}
