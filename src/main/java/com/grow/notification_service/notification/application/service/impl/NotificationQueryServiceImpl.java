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

	@Override
	@Transactional(readOnly = true)
	public Page<NotificationListItemResponse> getPage(Long memberId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return jpa.findPage(memberId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public long unreadCount(Long memberId) {
		return jpa.countUnread(memberId);
	}

	@Override
	@Transactional
	public int markAllRead(Long memberId) {
		return jpa.markAllRead(memberId);
	}

	@Override
	@Transactional
	public boolean markOneRead(Long memberId, Long id) {
		return jpa.markOneRead(memberId, id) > 0;
	}

	@Override
	@Transactional
	public boolean deleteOne(Long memberId, Long id) {
		return jpa.deleteByNotificationIdAndMemberId(id, memberId) > 0;
	}

	@Override
	@Transactional
	public int deleteOlderThan(Long memberId, LocalDateTime before) {
		return jpa.deleteOld(memberId, before);
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationListItemResponse> topUnread(Long memberId, int size) {
		int limit = Math.max(1, Math.min(size, 10));
		Pageable p = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
		return jpa.findTopUnread(memberId, p).getContent();
	}
}