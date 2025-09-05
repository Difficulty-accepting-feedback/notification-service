package com.grow.notification_service.notice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeCreateRequest(
	@NotBlank
	@Size(max = 200)
	String title,

	@NotBlank
	String content,

	boolean pinned
) {}