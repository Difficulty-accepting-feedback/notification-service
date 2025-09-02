package com.grow.notification_service.qna.infra.persistence.entity;

import java.time.LocalDateTime;

import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "qna_post")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaPostJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long postId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QnaType type;       // QUESTION/ANSWER

	private Long parentId;      // ANSWER일 경우 필수

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QnaStatus status;   // ACTIVE/DELETED

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) createdAt = LocalDateTime.now();
		if (status == null) status = QnaStatus.ACTIVE;
	}
	@PreUpdate
	void preUpdate() { updatedAt = LocalDateTime.now(); }
}