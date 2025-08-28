package com.grow.notification_service.note.infra.persistence.mapper;

import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.infra.persistence.entity.NoteJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public Note toDomain(NoteJpaEntity e) {
        return new Note(
            e.getNoteId(),
            e.getSenderId(),
            e.getRecipientId(),
            e.getContent(),
            e.getCreatedAt(),
            e.getIsRead(),
            e.getSenderDeleted(),
            e.getRecipientDeleted()
        );
    }

    public NoteJpaEntity toEntity(Note n) {
        return NoteJpaEntity.builder()
            .noteId(n.getNoteId())
            .senderId(n.getSenderId())
            .recipientId(n.getRecipientId())
            .content(n.getContent())
            .createdAt(n.getCreatedAt())
            .isRead(n.isRead())
            .senderDeleted(n.isSenderDeleted())
            .recipientDeleted(n.isRecipientDeleted())
            .build();
    }
}