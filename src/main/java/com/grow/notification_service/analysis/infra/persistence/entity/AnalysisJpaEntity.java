package com.grow.notification_service.analysis.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@Table(name = "analysis",
    indexes = {
        @Index(name="idx_analysis_session", columnList = "sessionId")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    @Column(name = "memberId", nullable = false)
    private Long memberId; // 멤버 ID

    @Column(name = "categoryId", nullable = false)
    private Long categoryId; // 카테고리 ID

    @Column(name = "sessionId", length = 64)
    private String sessionId; // 세션 ID

    @Column(name = "analysisResult", nullable = false, columnDefinition = "TEXT")
    private String analysisResult; // 분석 결과
}