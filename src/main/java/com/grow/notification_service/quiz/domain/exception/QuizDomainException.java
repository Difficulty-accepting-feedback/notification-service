package com.grow.notification_service.quiz.domain.exception;

import com.grow.notification_service.common.exception.DomainException;

public class QuizDomainException extends DomainException {

	public QuizDomainException(String message) {
		super(message);
	}

	public static QuizDomainException emptyQuestion() {
		return new QuizDomainException("문제 내용은 비어있을 수 없습니다.");
	}

	public static QuizDomainException emptyChoices() {
		return new QuizDomainException("보기는 최소 1개 이상이어야 합니다.");
	}

	public static QuizDomainException answerNotInChoices() {
		return new QuizDomainException("정답은 보기 중 하나여야 합니다.");
	}

	public static QuizDomainException levelRequired() {
		return new QuizDomainException("퀴즈 난이도는 반드시 지정해야 합니다.");
	}

	public static QuizDomainException categoryRequired() {
		return new QuizDomainException("퀴즈 카테고리는 반드시 지정해야 합니다.");
	}

	public static QuizDomainException invalidQuizId() {
		return new QuizDomainException("유효하지 않은 퀴즈 ID입니다.");
	}

	public static QuizDomainException submissionEmpty() {
		return new QuizDomainException("제출된 답변은 비어 있을 수 없습니다.");
	}
}