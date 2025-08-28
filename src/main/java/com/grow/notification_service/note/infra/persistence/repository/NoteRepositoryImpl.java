package com.grow.notification_service.note.infra.persistence.repository;

import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.domain.repository.NoteRepository;
import com.grow.notification_service.note.infra.persistence.entity.NoteJpaEntity;
import com.grow.notification_service.note.infra.persistence.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NoteRepositoryImpl implements NoteRepository {

    private final NoteMapper mapper;
    private final NoteJpaRepository jpa;

    @Override
    public Note save(Note note) {
        return mapper.toDomain(jpa.save(mapper.toEntity(note)));
    }

    @Override
    public Optional<Note> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Note> findSent(Long senderId, Pageable pageable) {
        return jpa.findBySenderIdAndSenderDeletedFalseOrderByCreatedAtDesc(senderId, pageable)
            .map(mapper::toDomain);
    }

    @Override
    public Page<Note> findReceived(Long recipientId, Pageable pageable) {
        return jpa.findByRecipientIdAndRecipientDeletedFalseOrderByCreatedAtDesc(recipientId, pageable)
            .map(mapper::toDomain);
    }

    @Override
    public long countUnread(Long recipientId) {
        return jpa.countByRecipientIdAndRecipientDeletedFalseAndIsReadFalse(recipientId);
    }

    @Override
    public void markRead(Long noteId, Long byMemberId) {
        NoteJpaEntity entity = jpa.findById(noteId).orElseThrow();
        Note domain = mapper.toDomain(entity);

        domain.markRead(byMemberId);

        jpa.save(mapper.toEntity(domain));
    }

    @Override
    public void softDelete(Long noteId, Long byMemberId) {
        NoteJpaEntity entity = jpa.findById(noteId).orElseThrow();
        Note domain = mapper.toDomain(entity);

        domain.deleteBy(byMemberId);

        if (domain.deletablePhysically()) {
            jpa.delete(entity);      // 판단은 도메인, 실행은 인프라
        } else {
            jpa.save(mapper.toEntity(domain));
        }
    }

    @Override
    public void deletePhysicallyIfBothDeleted(Long noteId) {
        jpa.findById(noteId).ifPresent(e -> {
            if (Boolean.TRUE.equals(e.getSenderDeleted()) && Boolean.TRUE.equals(e.getRecipientDeleted())) {
                jpa.delete(e);
            }
        });
    }
}