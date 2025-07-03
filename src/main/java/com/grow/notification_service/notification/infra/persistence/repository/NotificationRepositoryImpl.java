package com.grow.notification_service.notification.infra.persistence.repository;

import com.grow.notification_service.notification.domain.model.Notification;
import com.grow.notification_service.notification.domain.repository.NotificationRepository;
import com.grow.notification_service.notification.infra.persistence.entity.NotificationJpaEntity;
import com.grow.notification_service.notification.infra.persistence.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationMapper mapper;
    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = mapper.toEntity(notification);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
