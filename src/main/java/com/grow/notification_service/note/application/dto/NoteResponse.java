package com.grow.notification_service.note.application.dto;

import java.time.LocalDateTime;

import com.grow.notification_service.note.domain.model.Note;

public record NoteResponse(
	Long noteId,
	Long senderId,
	Long recipientId,
	String senderNickname,
	String recipientNickname,
	String content,
	boolean isRead,
	LocalDateTime createdAt
) {
	public static NoteResponse from(Note n) {
		return new NoteResponse(
			n.getNoteId(),
			n.getSenderId(),
			n.getRecipientId(),
			n.getSenderNickname(),
			n.getRecipientNickname(),
			n.getContent(),
			n.isRead(),
			n.getCreatedAt()
		);
	}
}