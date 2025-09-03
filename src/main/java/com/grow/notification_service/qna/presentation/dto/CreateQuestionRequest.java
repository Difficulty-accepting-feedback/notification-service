package com.grow.notification_service.qna.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateQuestionRequest(
	@NotBlank String content,
	Long parentId // null이면 루트 질문, 값이 있으면 추가 질문
) {}