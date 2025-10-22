package com.grow.notification_service.quiz.infra.persistence.entity;

import java.util.List;

import org.hibernate.annotations.Type;

import com.grow.notification_service.quiz.infra.persistence.enums.QuizLevel;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "quiz")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizJpaEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long quizId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String question;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String answer;

	@Column(name = "explanation", columnDefinition = "TEXT")
	private String explain;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuizLevel level;

	@Column(nullable = false)
	private Long categoryId;

	@Type(JsonType.class)
	@Column(columnDefinition = "json", nullable = false)
	private List<String> choices;
}