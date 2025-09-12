package com.grow.notification_service.quiz.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;

public interface QuizRepository {

	Quiz save(Quiz quiz);

	Optional<Quiz> findById(Long quizId);

	boolean existsByCategoryIdAndQuestion(Long categoryId, String question);

	List<Quiz> pick(Long categoryId, QuizLevel level, List<Long> excludedQuizIds, Pageable pageable);
}