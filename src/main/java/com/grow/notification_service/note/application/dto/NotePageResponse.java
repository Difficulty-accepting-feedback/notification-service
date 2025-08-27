package com.grow.notification_service.note.application.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.grow.notification_service.note.domain.model.Note;

public record NotePageResponse(
	int page,
	int size,
	long totalElements,
	int totalPages,
	List<NoteResponse> content
) {
	public static NotePageResponse from(Page<Note> p) {
		List<NoteResponse> mapped = p.map(NoteResponse::from).getContent();
		return new NotePageResponse(
			p.getNumber(),
			p.getSize(),
			p.getTotalElements(),
			p.getTotalPages(),
			mapped
		);
	}
}