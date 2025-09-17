package com.grow.notification_service.analysis.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "keyword_concept",
	uniqueConstraints = @UniqueConstraint(name = "uq_keyword_concept", columnNames = "keyword_normalized"))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordConceptJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long keywordId;

	@Column(name = "keyword_normalized", nullable = false)
	private String keywordNormalized;

	@Column(name = "keyword_original")
	private String keywordOriginal;

	@Lob
	@Column(name = "concept_summary", nullable = false)
	private String conceptSummary;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;
}