package com.grow.notification_service.notification.application.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.notification.application.dto.NotificationListItemResponse;
import com.grow.notification_service.notification.application.service.NotificationQueryService;
import com.grow.notification_service.notification.infra.persistence.repository.NotificationJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {

	private final NotificationJpaRepository jpa;

	/**
	 *  <h2>알림 페이지 조회</h2>
	 *  사용자의 알림 목록을 페이지 단위로 조회합니다.
	 * @param memberId
	 * @param page
	 * @param size
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<NotificationListItemResponse> getPage(Long memberId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return jpa.findPage(memberId, pageable);
	}

	/**
	 * <h2>읽지 않은 알림 개수 조회</h2>
	 * 사용자의 읽지 않은 알림 개수를 조회합니다.
	 * @param memberId
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public long unreadCount(Long memberId) {
		return jpa.countUnread(memberId);
	}

	/**
	 * <h2>모든 알림을 읽음으로 표시</h2>
	 * 사용자의 모든 알림을 읽음 상태로 변경합니다.
	 * @param memberId
	 * @return
	 */
	@Override
	@Transactional
	public int markAllRead(Long memberId) {
		return jpa.markAllRead(memberId);
	}

	/**
	 * <h2>특정 알림을 읽음으로 표시</h2>
	 * 사용자의 특정 알림을 읽음 상태로 변경합니다.
	 * @param memberId
	 * @param id
	 * @return
	 */
	@Override
	@Transactional
	public boolean markOneRead(Long memberId, Long id) {
		return jpa.markOneRead(memberId, id) > 0;
	}

	/**
	 * <h2>특정 알림 삭제</h2>
	 * 사용자의 특정 알림을 삭제합니다.
	 * @param memberId
	 * @param id
	 * @return
	 */
	@Override
	@Transactional
	public boolean deleteOne(Long memberId, Long id) {
		return jpa.deleteByNotificationIdAndMemberId(id, memberId) > 0;
	}

	/**
	 * <h2>특정 날짜 이전의 알림 삭제</h2>
	 * 사용자의 특정 날짜 이전의 알림을 삭제합니다.
	 * @param memberId
	 * @param before
	 * @return
	 */
	@Override
	@Transactional
	public int deleteOlderThan(Long memberId, LocalDateTime before) {
		return jpa.deleteOld(memberId, before);
	}

	/**
	 * <h2>최신 읽지 않은 알림 목록 조회</h2>
	 * 사용자의 최신 읽지 않은 알림 목록을 조회합니다.
	 * @param memberId
	 * @param size
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public List<NotificationListItemResponse> topUnread(Long memberId, int size) {
		int limit = Math.max(1, Math.min(size, 10));
		Pageable p = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
		return jpa.findTopUnread(memberId, p).getContent();
	}
}