package com.grow.notification_service.note.infra.persistence.repository;

import com.grow.notification_service.note.infra.persistence.entity.NoteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteJpaRepository extends JpaRepository<NoteJpaEntity, Long> {
}
