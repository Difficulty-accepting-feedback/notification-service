package com.grow.notification_service.analysis.infra.persistence.repository;


import java.util.Optional;

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

    /**
     * 주어진 Analysis 객체를 저장합니다.
     * @param analysis 저장할 Analysis 도메인 객체
     * @return 데이터베이스에 저장된 후의 Analysis 객체
     */
    @Override
    public Analysis save(Analysis analysis) {
        AnalysisJpaEntity entity = mapper.toEntity(analysis);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    /**
     * 주어진 카테고리 ID와 세션 ID를 기준으로 가장 최근에 생성된 Analysis 객체를 찾습니다.

     * @param categoryId 카테고리 ID
     * @param sessionId 분석을 찾을 세션의 고유 ID.
     * @return 가장 최신 Analysis 객체를 담고 있는 Optional. 없을 경우 Optional.empty().
     */
    @Override
    public Optional<Analysis> findTopByCategoryIdAndSessionIdOrderByAnalysisIdDesc(Long categoryId, String sessionId) {
        return jpaRepository.findTopByCategoryIdAndSessionIdOrderByAnalysisIdDesc(categoryId, sessionId)
            .map(mapper::toDomain);
    }
}