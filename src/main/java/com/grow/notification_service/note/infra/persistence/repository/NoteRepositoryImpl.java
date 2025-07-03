package com.grow.notification_service.note.infra.persistence.repository;

import com.grow.notification_service.note.domain.model.Note;
import com.grow.notification_service.note.domain.repository.NoteRepository;
import com.grow.notification_service.note.infra.persistence.entity.NoteJpaEntity;
import com.grow.notification_service.note.infra.persistence.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NoteRepositoryImpl implements NoteRepository {

    private final NoteMapper mapper;
    private final NoteJpaRepository jpaRepository;

    @Override
    public Note save(Note note) {
        NoteJpaEntity entity = mapper.toEntity(note);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
