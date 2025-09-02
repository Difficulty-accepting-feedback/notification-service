package com.grow.ai_coaching_service.question.infra.persistence.mapper;

import com.grow.ai_coaching_service.question.domain.model.Question;
import com.grow.ai_coaching_service.question.infra.persistence.entity.QuestionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    // 엔티티 -> 도메인 (전체 내역 반환)
    public Question toDomain(QuestionJpaEntity entity) {
        return new Question(
                entity.getQuestionId(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    // 도메인 -> 엔티티 (최초 저장 시에 활용)
    public QuestionJpaEntity toEntity(Question question) {
        return QuestionJpaEntity.builder()
                .content(question.getContent())
                .createdAt(question.getCreatedAt())
                .build();
    }
}
