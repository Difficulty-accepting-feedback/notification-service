package com.grow.notification_service.notice.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.grow.notification_service.notice.domain.model.Notice;
import com.grow.notification_service.notice.presentation.dto.NoticeCreateRequest;
import com.grow.notification_service.notice.presentation.dto.NoticeEditRequest;
import com.grow.notification_service.notice.presentation.dto.NoticePinRequest;

public interface NoticeApplicationService {

	Notice create(Long memberId, NoticeCreateRequest req);

	Notice edit(Long memberId, Long id, NoticeEditRequest req);

	Notice setPinned(Long memberId, Long id, NoticePinRequest req);

	void delete(Long memberId, Long id);

	Notice get(Long id);

	Page<Notice> getPage(Pageable pageable);
}