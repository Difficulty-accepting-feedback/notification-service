package com.grow.notification_service.note.infra.persistence.mapper;

import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.infra.persistence.entity.NoteJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    // 엔티티 -> 도메인
    public Note toDomain(NoteJpaEntity entity) {
        return new Note(
                entity.getNoteId(),
                entity.getSenderId(),
                entity.getRecipientId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getIsRead()
        );
    }

    // 도메인 -> 엔티티
    public NoteJpaEntity toEntity(Note note) {
        return NoteJpaEntity.builder()
                .senderId(note.getSenderId())
                .recipientId(note.getRecipientId())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .isRead(note.getIsRead())
                .build();
    }
}
