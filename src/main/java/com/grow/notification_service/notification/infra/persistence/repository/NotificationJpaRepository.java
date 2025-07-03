package com.grow.notification_service.notification.infra.persistence.repository;

import com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {
}
