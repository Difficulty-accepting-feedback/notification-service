package com.grow.notification_service.analysis.infra.persistence.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MemberLatestAiReviewId implements Serializable {
	private Long memberId;
	private Long categoryId;
}