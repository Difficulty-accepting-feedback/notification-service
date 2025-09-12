package com.grow.notification_service.notice.presentation.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grow.notification_service.global.dto.RsData;
import com.grow.notification_service.notice.application.dto.NoticeResponse;
import com.grow.notification_service.notice.application.service.NoticeApplicationService;
import com.grow.notification_service.notice.domain.model.Notice;
import com.grow.notification_service.notice.presentation.dto.NoticeCreateRequest;
import com.grow.notification_service.notice.presentation.dto.NoticeEditRequest;
import com.grow.notification_service.notice.presentation.dto.NoticePinRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification/notices")
@Tag(name = "NOTICE", description = "공지 API")
@Validated
public class NoticeController {

	private final NoticeApplicationService service; // 인터페이스 의존

	@Operation(summary = "공지 생성(관리자)")
	@PostMapping
	public ResponseEntity<RsData<NoticeResponse>> create(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@Valid @RequestBody NoticeCreateRequest req
	) {
		Notice saved = service.create(memberId, req.title(), req.content(), req.pinned());
		return ResponseEntity
			.created(URI.create("/api/v1/notices/" + saved.getNoticeId()))
			.body(new RsData<>("201", "공지 생성 성공", NoticeResponse.from(saved)));
	}

	@Operation(summary = "공지 수정(관리자)")
	@PutMapping("/{id}")
	public ResponseEntity<RsData<NoticeResponse>> edit(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long id,
		@Valid @RequestBody NoticeEditRequest req
	) {
		Notice edited = service.edit(memberId, id, req.title(), req.content());
		return ResponseEntity.ok(new RsData<>("200", "공지 수정 성공", NoticeResponse.from(edited)));
	}

	@Operation(summary = "공지 고정/해제(관리자)")
	@PatchMapping("/{id}/pinned")
	public ResponseEntity<RsData<NoticeResponse>> setPinned(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long id,
		@RequestBody NoticePinRequest req
	) {
		Notice pinned = service.setPinned(memberId, id, req.pinned());
		return ResponseEntity.ok(new RsData<>("200", "공지 고정 상태 변경 성공", NoticeResponse.from(pinned)));
	}

	@Operation(summary = "공지 삭제(관리자)")
	@DeleteMapping("/{id}")
	public ResponseEntity<RsData<Void>> delete(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long id
	) {
		service.delete(memberId, id);
		return ResponseEntity.ok(new RsData<>("200", "공지 삭제 성공", null));
	}

	@Operation(summary = "공지 단건 조회")
	@GetMapping("/{id}")
	public ResponseEntity<RsData<NoticeResponse>> get(@PathVariable Long id) {
		Notice n = service.get(id);
		return ResponseEntity.ok(new RsData<>("200", "공지 조회 성공", NoticeResponse.from(n)));
	}

	@Operation(summary = "공지 목록 조회 (고정 우선 -> 최신순)")
	@GetMapping
	public ResponseEntity<RsData<Page<NoticeResponse>>> getPage(Pageable pageable) {
		Page<NoticeResponse> page = service.getPage(pageable).map(NoticeResponse::from);
		return ResponseEntity.ok(new RsData<>("200", "공지 목록 조회 성공", page));
	}
}