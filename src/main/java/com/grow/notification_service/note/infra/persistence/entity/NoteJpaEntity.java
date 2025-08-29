package com.grow.notification_service.note.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "note",
    indexes = {
        @Index(name = "idx_note_sender_createdAt", columnList = "senderId, createdAt DESC"),
        @Index(name = "idx_note_recipient_createdAt", columnList = "recipientId, createdAt DESC"),
        @Index(name = "idx_note_isRead", columnList = "recipientId, isRead")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noteId", updatable = false, nullable = false)
    private Long noteId;

    @Column(name = "senderId", nullable = false)
    private Long senderId; // 발신자 멤버 ID

    @Column(name = "recipientId", nullable = false)
    private Long recipientId; // 수신자 멤버 ID

    @Column(name = "senderNickname", nullable = false, length = 60)
    private String senderNickname; // 전송 시점 발신자 닉네임

    @Column(name = "recipientNickname", nullable = false, length = 60)
    private String recipientNickname; // 전송 시점 수신자 닉네임

    @Column(name = "content", nullable = false, length = 2000)
    private String content; // 쪽지 내용

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt; // 생성 시간

    @Column(name = "isRead", nullable = false)
    private Boolean isRead; // 조회 여부

    // 소프트 삭제 플래그
    @Column(name = "senderDeleted", nullable = false)
    private Boolean senderDeleted;

    @Column(name = "recipientDeleted", nullable = false)
    private Boolean recipientDeleted;
}