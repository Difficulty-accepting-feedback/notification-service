package com.grow.notification_service.notice.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.notice.infra.persistence.entity.NoticeJpaEntity;

public interface NoticeJpaRepository
	extends JpaRepository<NoticeJpaEntity, Long> {
}