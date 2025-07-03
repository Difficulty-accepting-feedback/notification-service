package com.grow.notification_service.note.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "note")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noteId", updatable = false, nullable = false)
    private Long noteId;

    @Column(name = "senderId",  updatable = false, nullable = false)
    private Long senderId; // 발신자 멤버 ID

    @Column(name = "senderId",  updatable = false, nullable = false)
    private Long recipientId; // 수신자 멤버 ID

    @Column(name = "senderId",  updatable = false, nullable = false)
    private String content; // 쪽지 내용

    @Column(name = "senderId",  updatable = false, nullable = false)
    private LocalDateTime createdAt; // 생성 시간

    @Column(name = "senderId",  updatable = false, nullable = false)
    private Boolean isRead; // 조회 여부

    @Builder
    public NoteJpaEntity(Long senderId,
                         Long recipientId,
                         String content,
                         LocalDateTime createdAt,
                         Boolean isRead
    ) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }
}
