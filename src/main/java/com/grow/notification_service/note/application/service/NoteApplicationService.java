package com.grow.notification_service.note.application.service;

import com.grow.notification_service.note.application.dto.NotePageResponse;
import com.grow.notification_service.note.application.dto.NoteResponse;
import com.grow.notification_service.note.presentation.dto.SendNoteRequest;
import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.domain.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteApplicationService {

	private final NoteRepository noteRepository;

	/**
	 * 새 쪽지 생성, 저장
	 * 도메인 팩토리를 통해 불변 조건을 검증하고 저장
	 */
	@Transactional
	public NoteResponse send(Long senderId, SendNoteRequest req) {
		Note note = Note.create(senderId, req.recipientId(), req.content());
		Note saved = noteRepository.save(note);
		return NoteResponse.from(saved);
	}

	/**
	 * 받은 쪽지함(수신자 기준) 페이지 단위로 조회
	 * 소프트 딜리트된 항목은 제외, 최신 생성일 기준으로 정렬
	 */
	@Transactional(readOnly = true)
	public NotePageResponse inbox(Long memberId, int page, int size) {
		Page<Note> notePage = noteRepository.findReceived(memberId, PageRequest.of(page, size));
		return NotePageResponse.from(notePage);
	}

	/**
	 * 보낸 쪽지함(발신자 기준) 페이지 단위 조회
	 * 소프트 딜리트된 항목은 제외, 최신 생성일 기준으로 정렬
	 */
	@Transactional(readOnly = true)
	public NotePageResponse outbox(Long memberId, int page, int size) {
		Page<Note> notePage = noteRepository.findSent(memberId, PageRequest.of(page, size));
		return NotePageResponse.from(notePage);
	}

	/**
	 * 수신자 쪽지 읽음 처리
	 * 권한 검증을 수행하며 수신자만 읽음 처리가 가능
	 */
	@Transactional
	public void markRead(Long memberId, Long noteId) {
		noteRepository.markRead(noteId, memberId);
	}

	/**
	 * 쪽지 삭제
	 * 우선 소프트 딜리트 후, 발신자와 수신자 모두 삭제했을 경우 물리 삭제
	 */
	@Transactional
	public void delete(Long memberId, Long noteId) {
		noteRepository.softDelete(noteId, memberId);
		noteRepository.deletePhysicallyIfBothDeleted(noteId);
	}

	/**
	 * 현재 회원이 받은 쪽지 중 읽지 않은 개수를 반환
	 * 소프트 딜리트된 항목은 제외
	 */
	@Transactional(readOnly = true)
	public long unreadCount(Long memberId) {
		return noteRepository.countUnread(memberId);
	}
}