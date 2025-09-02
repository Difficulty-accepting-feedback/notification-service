package com.grow.ai_coaching_service.analysis.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    @Column(name = "memberId", nullable = false)
    private Long memberId; // 멤버 ID

    @Column(name = "categoryId", nullable = false)
    private Long categoryId; // 카테고리 ID

    @Column(name = "analysisResult", nullable = false, columnDefinition = "TEXT")
    private String analysisResult; // 분석 결과

    @Builder
    public AnalysisJpaEntity(Long memberId,
                             Long categoryId,
                             String analysisResult
    ) {
        this.memberId = memberId;
        this.categoryId = categoryId;
        this.analysisResult = analysisResult;
    }
}
