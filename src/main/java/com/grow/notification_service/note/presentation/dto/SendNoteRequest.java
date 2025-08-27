package com.grow.notification_service.note.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendNoteRequest(
	@NotNull
	Long recipientId,
	@NotBlank
	String content
) {}