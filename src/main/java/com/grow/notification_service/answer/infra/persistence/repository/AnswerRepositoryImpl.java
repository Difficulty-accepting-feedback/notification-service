package com.grow.ai_coaching_service.answer.infra.persistence.repository;

import com.grow.ai_coaching_service.answer.domain.model.Answer;
import com.grow.ai_coaching_service.answer.domain.repository.AnswerRepository;
import com.grow.ai_coaching_service.answer.infra.persistence.entity.AnswerJpaEntity;
import com.grow.ai_coaching_service.answer.infra.persistence.mapper.AnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnswerRepositoryImpl implements AnswerRepository {

    private final AnswerMapper mapper;
    private final AnswerJpaRepository jpaRepository;

    @Override
    public Answer save(Answer answer) {
        AnswerJpaEntity entity = mapper.toEntity(answer);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
