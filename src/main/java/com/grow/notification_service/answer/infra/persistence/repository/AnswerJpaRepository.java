package com.grow.ai_coaching_service.answer.infra.persistence.repository;

import com.grow.ai_coaching_service.answer.infra.persistence.entity.AnswerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerJpaRepository extends JpaRepository<AnswerJpaEntity,Long> {
}
