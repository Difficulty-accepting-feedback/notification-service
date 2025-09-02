package com.grow.ai_coaching_service.answer.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answerId", nullable = false)
    private Long answerId;

    @Column(name = "questionId",  nullable = false)
    private Long questionId; // 문의 ID

    @Column(name = "content",  nullable = false, columnDefinition = "TEXT")
    private String content; // 문의에 대한 답변 내용

    @Column(name = "createdAt",  nullable = false)
    private LocalDateTime createdAt; // 문의 답변 시각

    @Builder
    public AnswerJpaEntity(Long questionId,
                           String content,
                           LocalDateTime createdAt
    ) {
        this.questionId = questionId;
        this.content = content;
        this.createdAt = createdAt;
    }
}
