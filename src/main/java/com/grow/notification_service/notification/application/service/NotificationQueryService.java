package com.grow.notification_service.notification.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.grow.notification_service.notification.application.dto.NotificationListItemResponse;

public interface NotificationQueryService {

	/** 페이징 목록 조회 (최신순) */
	Page<NotificationListItemResponse> getPage(Long memberId, int page, int size);

	/** 읽지 않은 알림 카운트 */
	long unreadCount(Long memberId);

	/** 모두 읽음 처리 */
	int markAllRead(Long memberId);

	/** 단건 읽음 처리 */
	boolean markOneRead(Long memberId, Long id);

	/** 단건 삭제 */
	boolean deleteOne(Long memberId, Long id);

	/** 특정 시점 이전 전체 삭제 */
	int deleteOlderThan(Long memberId, LocalDateTime before);

	/** 읽지 않은 알림 목록 조회 (최신순) */
	List<NotificationListItemResponse> topUnread(Long memberId, int size);
}