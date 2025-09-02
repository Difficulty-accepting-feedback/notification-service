package com.grow.notification_service.qna.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAnswerRequest(
	@NotBlank(message = "내용은 비어 있을 수 없습니다.")
	String content
) {}