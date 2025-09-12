package com.grow.notification_service.notification.presentation.controller;

import com.grow.notification_service.notification.application.dto.NotificationListItemResponse;
import com.grow.notification_service.notification.application.dto.PageResponse;
import com.grow.notification_service.notification.application.dto.UnreadCountResponse;
import com.grow.notification_service.notification.application.service.NotificationQueryService;
import com.grow.notification_service.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
public class NotificationQueryController {

	private final NotificationQueryService queryService;

	@GetMapping
	public RsData<PageResponse<NotificationListItemResponse>> list(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Page<NotificationListItemResponse> p = queryService.getPage(memberId, page, size);
		PageResponse<NotificationListItemResponse> body = PageResponse.<NotificationListItemResponse>builder()
			.items(p.getContent())
			.page(p.getNumber())
			.size(p.getSize())
			.totalElements(p.getTotalElements())
			.totalPages(p.getTotalPages())
			.last(p.isLast())
			.build();
		return new RsData<>("200", "알림 목록 조회 성공", body);
	}

	@GetMapping("/unread-count")
	public RsData<UnreadCountResponse> unreadCount(@RequestHeader("X-Authorization-Id") Long memberId) {
		long c = queryService.unreadCount(memberId);
		return new RsData<>("200", "읽지 않은 메세지 조회 성공", new UnreadCountResponse(c));
	}

	@PostMapping("/read-all")
	public RsData<Integer> markAllRead(@RequestHeader("X-Authorization-Id") Long memberId) {
		int updated = queryService.markAllRead(memberId);
		return new RsData<>("200", "모두 읽음 처리 완료", updated);
	}

	@PostMapping("/{id}/read")
	public RsData<Void> markOneRead(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long id
	) {
		boolean ok = queryService.markOneRead(memberId, id);
		return new RsData<>(ok ? "200" : "404", ok ? "읽음 처리 완료" : "대상이 없습니다.", null);
	}

	@DeleteMapping("/{id}")
	public RsData<Void> deleteOne(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long id
	) {
		boolean ok = queryService.deleteOne(memberId, id);
		return new RsData<>(ok ? "200" : "404", ok ? "삭제 완료" : "대상이 없습니다.", null);
	}

	@DeleteMapping("/older-than")
	public RsData<Integer> deleteOlderThan(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before
	) {
		int removed = queryService.deleteOlderThan(memberId, before);
		return new RsData<>("200", "오래된 알림 삭제 완료", removed);
	}

	@GetMapping("/top")
	public RsData<List<NotificationListItemResponse>> topUnread(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam(defaultValue = "5") int size
	) {
		return new RsData<>("200", "안 읽은 메세지 반환 완료", queryService.topUnread(memberId, size));
	}
}