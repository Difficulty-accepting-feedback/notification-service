package com.grow.notification_service.notice.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.grow.notification_service.notice.domain.model.Notice;

public interface NoticeRepository {
	Notice save(Notice notice);
	Optional<Notice> findById(Long id);
	void deleteById(Long id);
	Page<Notice> findPinnedFirstOrderByCreatedAtDesc(Pageable pageable);
}