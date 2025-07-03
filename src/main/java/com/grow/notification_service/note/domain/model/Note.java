package com.grow.notification_service.note.domain.model;

import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
public class Note {

    private Long noteId;
    private Long senderId; // 발신자 멤버 ID
    private Long recipientId; // 수신자 멤버 ID
    private String content; // 쪽지 내용
    private LocalDateTime createdAt; // 생성 시간
    private Boolean isRead; // 조회 여부

    public Note(Long senderId,
                Long recipientId,
                String content,
                Clock createdAt
    ) {
        this.noteId = null;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.isRead = false; // 기본 값은 읽지 않음

        if (createdAt != null) {
            this.createdAt = LocalDateTime.now(createdAt);
        } else {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Note(Long noteId,
                Long senderId,
                Long recipientId,
                String content,
                LocalDateTime createdAt,
                Boolean isRead
    ) {
        this.noteId = noteId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }
}
