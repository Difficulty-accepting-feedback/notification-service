package com.grow.ai_coaching_service.answer.infra.persistence.mapper;

import com.grow.ai_coaching_service.answer.domain.model.Answer;
import com.grow.ai_coaching_service.answer.infra.persistence.entity.AnswerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AnswerMapper {

    // 엔티티 -> 도메인
    public Answer toDomain(AnswerJpaEntity entity) {
        return new Answer(
                entity.getAnswerId(),
                entity.getQuestionId(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    // 도메인 -> 엔티티
    public AnswerJpaEntity toEntity(Answer answer) {
        return AnswerJpaEntity.builder()
                .questionId(answer.getQuestionId())
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
