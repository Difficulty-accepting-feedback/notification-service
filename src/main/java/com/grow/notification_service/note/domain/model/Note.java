package com.grow.notification_service.note.domain.model;

import com.grow.notification_service.note.domain.exception.NoteDomainException;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Note {

    private final Long noteId;
    private final Long senderId;        // 발신자 멤버 ID
    private final Long recipientId;     // 수신자 멤버 ID
    private String content;             // 쪽지 내용
    private final LocalDateTime createdAt; // 생성 시간
    private boolean isRead;             // 조회 여부 (수신자 기준)
    private boolean senderDeleted;      // 발신자 소프트 삭제
    private boolean recipientDeleted;   // 수신자 소프트 삭제

    // 새 쪽지 생성
    public static Note create(Long senderId, Long recipientId, String content) {
        if (senderId == null) {
            throw NoteDomainException.senderIdRequired();
        }
        if (recipientId == null) {
            throw NoteDomainException.recipientIdRequired();
        }
        if (senderId.equals(recipientId)) {
            throw NoteDomainException.selfSendNotAllowed();
        }
        if (content == null || content.isBlank()) {
            throw NoteDomainException.emptyContent();
        }
        return new Note(
            null, senderId, recipientId, content,
            LocalDateTime.now(),
            false, false, false
        );
    }

    public Note(Long noteId, Long senderId, Long recipientId, String content,
        LocalDateTime createdAt, Boolean isRead,
        Boolean senderDeleted, Boolean recipientDeleted) {
        this.noteId = noteId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead != null && isRead;
        this.senderDeleted = senderDeleted != null && senderDeleted;
        this.recipientDeleted = recipientDeleted != null && recipientDeleted;
    }

    // 비즈니스 로직

    // 읽음 처리
    public void markRead(Long memberId) {
        if (!recipientId.equals(memberId)) {
            throw NoteDomainException.readAllowedOnlyForRecipient();
        }
        this.isRead = true;
    }

    // 삭제 처리
    public void deleteBy(Long memberId) {
        if (senderId.equals(memberId)) {
            this.senderDeleted = true;
        } else if (recipientId.equals(memberId)) {
            this.recipientDeleted = true;
        } else {
            throw NoteDomainException.deletePermissionDenied();
        }
    }

    // 물리적 삭제 가능 여부
    public boolean deletablePhysically() {
        return senderDeleted && recipientDeleted;
    }
}