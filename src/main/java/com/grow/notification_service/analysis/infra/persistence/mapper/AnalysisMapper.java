package com.grow.notification_service.analysis.infra.persistence.mapper;


import org.springframework.stereotype.Component;

import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.infra.persistence.entity.AnalysisJpaEntity;

@Component
public class AnalysisMapper {

    // 엔티티를 도메인으로
    public Analysis toDomain(AnalysisJpaEntity e) {
        return new Analysis(
            e.getAnalysisId(),
            e.getMemberId(),
            e.getCategoryId(),
            e.getAnalysisResult());
    }

    // 도메인을 엔티티로
    public AnalysisJpaEntity toEntity(Analysis d) {
        return AnalysisJpaEntity.builder()
            .memberId(d.getMemberId())
            .categoryId(d.getCategoryId())
            .analysisResult(d.getAnalysisResult())
            .build();
    }
}