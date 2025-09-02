package com.grow.ai_coaching_service.question.infra.persistence.repository;

import com.grow.ai_coaching_service.question.infra.persistence.entity.QuestionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity,Long> {
}
