package com.grow.notification_service.quiz.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.quiz.infra.persistence.entity.QuizJpaEntity;

public interface QuizJpaRepository
	extends JpaRepository<QuizJpaEntity, Long> {
}