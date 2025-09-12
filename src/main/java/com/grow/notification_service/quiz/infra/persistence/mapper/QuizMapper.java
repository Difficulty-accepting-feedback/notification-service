package com.grow.notification_service.quiz.infra.persistence.mapper;

import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.infra.persistence.entity.QuizJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class QuizMapper {

	public Quiz toDomain(QuizJpaEntity e) {
		return new Quiz(
			e.getQuizId(),
			e.getQuestion(),
			e.getChoices(),
			e.getAnswer(),
			e.getExplain(),
			e.getLevel(),
			e.getCategoryId()
		);
	}

	public QuizJpaEntity toEntity(Quiz q) {
		return QuizJpaEntity.builder()
			.quizId(q.getQuizId())
			.question(q.getQuestion())
			.choices(q.getChoices())
			.answer(q.getAnswer())
			.explain(q.getExplain())
			.level(q.getLevel())
			.categoryId(q.getCategoryId())
			.build();
	}
}