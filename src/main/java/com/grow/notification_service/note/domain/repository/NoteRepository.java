package com.grow.notification_service.note.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.grow.notification_service.note.domain.model.Note;

public interface NoteRepository {
    Note save(Note note);
    Optional<Note> findById(Long id);

    // 보낸 쪽지함, 받은 쪽지함, 안읽은 쪽지 개수
    Page<Note> findSent(Long senderId, Pageable pageable);
    Page<Note> findReceived(Long recipientId, Pageable pageable);
    long countUnread(Long recipientId);

    // 쪽지 읽음 처리, 쪽지 소프트 딜리트, 양측 모두 소프트 딜리트 시 물리적 삭제
    void markRead(Long noteId, Long byMemberId);
    void softDelete(Long noteId, Long byMemberId);

    // 양측 모두 소프트 딜리트 시 물리적 삭제
    void deletePhysicallyIfBothDeleted(Long noteId);
}