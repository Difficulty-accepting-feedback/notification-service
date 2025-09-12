package com.grow.notification_service.quiz.domain.model;

import java.util.List;

import com.grow.notification_service.quiz.domain.exception.QuizDomainException;
import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;

import lombok.Getter;

@Getter
public class Quiz {

	private final Long quizId;
	private final String question;
	private final List<String> choices; // 보기
	private final String answer; // 정답 (보기 중 하나)
	private final String explain; // 해설
	private final QuizLevel level; // EASY/NORMAL/HARD
	private final Long categoryId; // 카테고리 매핑 (SkillTag → categoryId)

	// 새 퀴즈 생성
	public static Quiz create(String question,
		List<String> choices,
		String answer,
		String explain,
		QuizLevel level,
		Long categoryId) {
		if (question == null || question.isBlank()) throw QuizDomainException.emptyQuestion();
		if (choices == null || choices.isEmpty()) throw QuizDomainException.emptyChoices();
		if (answer == null || !choices.contains(answer)) throw QuizDomainException.answerNotInChoices();
		if (level == null) throw QuizDomainException.levelRequired();
		if (categoryId == null) throw QuizDomainException.categoryRequired();

		return new Quiz(
			null, question, choices, answer, explain, level, categoryId
		);
	}

	public Quiz(Long quizId,
		String question,
		List<String> choices,
		String answer,
		String explain,
		QuizLevel level,
		Long categoryId) {
		this.quizId = quizId;
		this.question = question;
		this.choices = List.copyOf(choices); // 불변 리스트
		this.answer = answer;
		this.explain = explain;
		this.level = level;
		this.categoryId = categoryId;
	}

	// 비즈니스 로직

	/** 정답 검증 */
	public boolean isCorrect(String submitted) {
		if (submitted == null) return false;
		return this.answer.equalsIgnoreCase(submitted.trim());
	}
}