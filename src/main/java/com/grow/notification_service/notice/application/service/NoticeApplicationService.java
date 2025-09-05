package com.grow.notification_service.notice.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.grow.notification_service.notice.domain.model.Notice;

public interface NoticeApplicationService {

	Notice create(Long memberId, String title, String content, boolean pinned);

	Notice edit(Long memberId, Long id, String title, String content);

	Notice setPinned(Long memberId, Long id, boolean pinned);

	void delete(Long memberId, Long id);

	Notice get(Long id);

	Page<Notice> getPage(Pageable pageable);
}