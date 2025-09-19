package com.grow.notification_service.analysis.infra.persistence.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MemberLatestAiReviewId implements Serializable {
	private Long memberId;
	private Long categoryId;
}