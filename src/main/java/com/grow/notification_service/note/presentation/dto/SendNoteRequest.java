package com.grow.notification_service.note.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record SendNoteRequest(
	@NotBlank
	String recipientNickname,
	@NotBlank
	String content
) {}