package com.grow.notification_service.note.application.service;

import com.grow.notification_service.note.application.dto.NotePageResponse;
import com.grow.notification_service.note.application.dto.NoteResponse;
import com.grow.notification_service.note.presentation.dto.SendNoteRequest;

public interface NoteApplicationService {

	NoteResponse send(Long senderId, SendNoteRequest req);

	NotePageResponse inbox(Long memberId, int page, int size);

	NotePageResponse outbox(Long memberId, int page, int size);

	void markRead(Long memberId, Long noteId);

	void delete(Long memberId, Long noteId);

	long unreadCount(Long memberId);
}