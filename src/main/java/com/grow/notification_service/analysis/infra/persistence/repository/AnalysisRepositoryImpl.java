package com.grow.notification_service.analysis.infra.persistence.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.domain.repository.AnalysisRepository;
import com.grow.notification_service.analysis.infra.persistence.entity.AnalysisJpaEntity;
import com.grow.notification_service.analysis.infra.persistence.mapper.AnalysisMapper;

@Repository
@RequiredArgsConstructor
public class AnalysisRepositoryImpl implements AnalysisRepository {

    private final AnalysisMapper mapper;
    private final AnalysisJpaRepository jpaRepository;

    @Override
    public Analysis save(Analysis analysis) {
        AnalysisJpaEntity entity = mapper.toEntity(analysis);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}