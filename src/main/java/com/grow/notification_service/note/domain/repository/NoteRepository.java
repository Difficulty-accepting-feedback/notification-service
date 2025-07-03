package com.grow.notification_service.note.domain.repository;

import com.grow.notification_service.note.domain.model.Note;

public interface NoteRepository {
    Note save(Note note);
}
