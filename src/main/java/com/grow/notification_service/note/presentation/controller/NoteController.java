package com.grow.notification_service.note.presentation.controller;

import com.grow.notification_service.note.application.dto.NotePageResponse;
import com.grow.notification_service.note.application.dto.NoteResponse;
import com.grow.notification_service.note.presentation.dto.SendNoteRequest;
import com.grow.notification_service.note.application.service.NoteApplicationService;
import com.grow.notification_service.notification.presentation.dto.rsdata.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notes")
public class NoteController {

	private final NoteApplicationService appService;

	/**
	 * 쪽지를 새로 전송한다.
	 *
	 * @param memberId 게이트웨이에서 전달된 회원 ID
	 * @param req      수신자와 내용이 담긴 요청 DTO
	 * @return         RsData<NoteResponse> (전송된 쪽지의 상세 정보)
	 */
	@PostMapping
	public RsData<NoteResponse> send(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestBody @Valid SendNoteRequest req
	) {
		NoteResponse data = appService.send(memberId, req);
		return new RsData<>(
			"200",
			"쪽지 전송이 완료되었습니다.",
			data
		);
	}

	/**
	 * 받은 쪽지함을 조회한다.
	 *
	 * @param memberId 게이트웨이에서 전달된 회원 ID
	 * @param page     조회할 페이지 번호 (0부터 시작)
	 * @param size     한 페이지 크기
	 * @return         RsData<NotePageResponse> (받은 쪽지 리스트 + 페이징 정보)
	 */
	@GetMapping("/inbox")
	public RsData<NotePageResponse> inbox(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "20") int size
	) {
		NotePageResponse data = appService.inbox(memberId, page, size);
		return new RsData<>(
			"200",
			"받은 쪽지함 조회에 성공했습니다.",
			data
		);
	}

	/**
	 * 보낸 쪽지함을 조회한다.
	 *
	 * @param memberId 게이트웨이에서 전달된 회원 ID
	 * @param page     조회할 페이지 번호 (0부터 시작)
	 * @param size     한 페이지 크기
	 * @return         RsData<NotePageResponse> (보낸 쪽지 리스트 + 페이징 정보)
	 */
	@GetMapping("/outbox")
	public RsData<NotePageResponse> outbox(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "20") int size
	) {
		NotePageResponse data = appService.outbox(memberId, page, size);
		return new RsData<>(
			"200",
			"보낸 쪽지함 조회에 성공했습니다.",
			data
		);
	}

	/**
	 * 특정 쪽지를 읽음 처리한다.
	 *
	 * @param memberId 게이트웨이에서 전달된 회원 ID
	 * @param noteId   읽음 처리할 쪽지 ID
	 * @return         RsData<Void> (본문 데이터 없이 처리 결과만 반환)
	 */
	@PostMapping("/{noteId}/read")
	public RsData<Void> markRead(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long noteId
	) {
		appService.markRead(memberId, noteId);
		return new RsData<>(
			"200",
			"쪽지를 읽음 처리했습니다.",
			null
		);
	}

	/**
	 * 특정 쪽지를 삭제한다.
	 * - 한쪽만 삭제 시 소프트 딜리트
	 * - 발신자와 수신자 모두 삭제 시 물리 삭제
	 *
	 * @param memberId 게이트웨이에서 전달된 회원 ID
	 * @param noteId   삭제할 쪽지 ID
	 * @return         RsData<Void> (본문 데이터 없이 처리 결과만 반환)
	 */
	@DeleteMapping("/{noteId}")
	public RsData<Void> delete(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long noteId
	) {
		appService.delete(memberId, noteId);
		return new RsData<>(
			"200",
			"쪽지를 삭제했습니다.",
			null
		);
	}

	/**
	 * 안 읽은 쪽지 개수를 조회한다.
	 *
	 * @param memberId 게이트웨이에서 전달된 회원 ID
	 * @return         RsData<Long> (읽지 않은 쪽지 개수)
	 */
	@GetMapping("/unread-count")
	public RsData<Long> unreadCount(
		@RequestHeader("X-Authorization-Id") Long memberId
	) {
		long data = appService.unreadCount(memberId);
		return new RsData<>(
			"200",
			"읽지 않은 쪽지 개수 조회에 성공했습니다.",
			data
		);
	}
}