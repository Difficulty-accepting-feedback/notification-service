package com.grow.ai_coaching_service.question.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "questionId",  nullable = false, updatable = false)
    private Long questionId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content; // 문의 내용

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt; // 문의 시각

    @Builder
    public QuestionJpaEntity(String content,
                             LocalDateTime createdAt
    ) {
        this.content = content;
        this.createdAt = createdAt;
    }
}
