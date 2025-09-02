package com.grow.ai_coaching_service.question.infra.persistence.repository;

import com.grow.ai_coaching_service.question.domain.model.Question;
import com.grow.ai_coaching_service.question.domain.repository.QuestionRepository;
import com.grow.ai_coaching_service.question.infra.persistence.entity.QuestionJpaEntity;
import com.grow.ai_coaching_service.question.infra.persistence.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuestionRepositoryImpl implements QuestionRepository {

    private final QuestionMapper mapper;
    private final QuestionJpaRepository jpaRepository;

    @Override
    public Question save(Question question) {
        QuestionJpaEntity entity = mapper.toEntity(question);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
