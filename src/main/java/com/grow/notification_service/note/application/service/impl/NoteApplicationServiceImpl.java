package com.grow.notification_service.note.application.service.impl;

import com.grow.notification_service.note.application.dto.NotePageResponse;
import com.grow.notification_service.note.application.dto.NoteResponse;
import com.grow.notification_service.note.application.port.MemberPort;
import com.grow.notification_service.note.application.service.NoteApplicationService;
import com.grow.notification_service.note.presentation.dto.SendNoteRequest;
import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.domain.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteApplicationServiceImpl implements NoteApplicationService {

	private final NoteRepository noteRepository;
	private final MemberPort memberPort;

	/**
	 * 새 쪽지 생성, 저장
	 * 도메인 팩토리를 통해 불변 조건을 검증하고 저장
	 */
	@Override
	@Transactional
	public NoteResponse send(Long senderId, SendNoteRequest req) {
		// 닉네임 -> memberId 변환
		MemberPort.ResolveResult resolved =
			memberPort.resolveByNickname(req.recipientNickname());

		// 전송 시점 닉네임 확보
		String senderNick = memberPort.getMemberName(senderId);
		String recipientNick = resolved.nickname();

		// 저장 (스냅샷)
		Note saved = noteRepository.save(
			Note.create(senderId, resolved.memberId(), req.content(), senderNick, recipientNick)
		);

		log.info("[쪽지] 전송 완료 - senderId={}, recipientId={}, noteId={}",
			senderId, saved.getRecipientId(), saved.getNoteId());

		return NoteResponse.from(saved);
	}

	/**
	 * 받은 쪽지함(수신자 기준) 페이지 단위로 조회
	 * 소프트 딜리트된 항목은 제외, 최신 생성일 기준으로 정렬
	 */
	@Override
	@Transactional(readOnly = true)
	public NotePageResponse inbox(Long memberId, int page, int size) {
		Page<Note> notePage = noteRepository.findReceived(memberId, PageRequest.of(page, size));

		log.debug("[쪽지] 받은 쪽지함 조회 - memberId={}, page={}, size={}, totalElements={}",
			memberId, page, size, notePage.getTotalElements());

		return NotePageResponse.from(notePage);
	}

	/**
	 * 보낸 쪽지함(발신자 기준) 페이지 단위 조회
	 * 소프트 딜리트된 항목은 제외, 최신 생성일 기준으로 정렬
	 */
	@Override
	@Transactional(readOnly = true)
	public NotePageResponse outbox(Long memberId, int page, int size) {
		Page<Note> notePage = noteRepository.findSent(memberId, PageRequest.of(page, size));

		log.debug("[쪽지] 보낸 쪽지함 조회 - memberId={}, page={}, size={}, totalElements={}",
			memberId, page, size, notePage.getTotalElements());

		return NotePageResponse.from(notePage);
	}

	/**
	 * 수신자 쪽지 읽음 처리
	 * 권한 검증을 수행하며 수신자만 읽음 처리가 가능
	 */
	@Override
	@Transactional
	public void markRead(Long memberId, Long noteId) {
		noteRepository.markRead(noteId, memberId);
		log.info("[쪽지] 읽음 처리 - memberId={}, noteId={}", memberId, noteId);
	}

	/**
	 * 쪽지 삭제
	 * 우선 소프트 딜리트 후, 발신자와 수신자 모두 삭제했을 경우 물리 삭제
	 */
	@Override
	@Transactional
	public void delete(Long memberId, Long noteId) {
		log.debug("[쪽지] 삭제 요청 - memberId={}, noteId={}", memberId, noteId);

		noteRepository.softDelete(noteId, memberId);
		noteRepository.deletePhysicallyIfBothDeleted(noteId);

		log.info("[쪽지] 삭제 처리 완료 - memberId={}, noteId={}", memberId, noteId);
	}

	/**
	 * 현재 회원이 받은 쪽지 중 읽지 않은 개수를 반환
	 * 소프트 딜리트된 항목은 제외
	 */
	@Override
	@Transactional(readOnly = true)
	public long unreadCount(Long memberId) {
		long count = noteRepository.countUnread(memberId);
		log.debug("[쪽지] 미읽음 카운트 조회 - memberId={}, count={}", memberId, count);
		return count;
	}
}