package com.grow.notification_service.analysis.infra.persistence.mapper;


import org.springframework.stereotype.Component;

import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.infra.persistence.entity.AnalysisJpaEntity;

@Component
public class AnalysisMapper {

    // 엔티티를 도메인으로
    public Analysis toDomain(AnalysisJpaEntity entity) {
        return new Analysis(entity.getMemberId(),
                entity.getCategoryId(),
                entity.getAnalysisResult()
        );
    }

    // 도메인을 엔티티로
    public AnalysisJpaEntity toEntity(Analysis domain) {
        return AnalysisJpaEntity.builder()
                .memberId(domain.getMemberId())
                .categoryId(domain.getCategoryId())
                .analysisResult(domain.getAnalysisResult())
                .build();
    }
}