package com.grow.notification_service.qna.infra.persistence.mapper;

import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.infra.persistence.entity.QnaPostJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("[Infra][Mapper] QnaPostMapper 테스트")
class QnaPostMapperTest {

	@Test
	@DisplayName("엔티티 -> 도메인 변환 성공")
	void toDomain_success() {
		LocalDateTime now = LocalDateTime.now();
		QnaPostJpaEntity entity = QnaPostJpaEntity.builder()
			.postId(1L)
			.type(QnaType.QUESTION)
			.parentId(null)
			.memberId(10L)
			.content("도메인 매핑 테스트")
			.status(QnaStatus.ACTIVE)
			.createdAt(now)
			.updatedAt(now.plusMinutes(1))
			.build();

		QnaPost domain = QnaPostMapper.toDomain(entity);

		assertAll(
			() -> assertEquals(entity.getPostId(), domain.getId()),
			() -> assertEquals(entity.getType(), domain.getType()),
			() -> assertEquals(entity.getParentId(), domain.getParentId()),
			() -> assertEquals(entity.getMemberId(), domain.getMemberId()),
			() -> assertEquals(entity.getContent(), domain.getContent()),
			() -> assertEquals(entity.getStatus(), domain.getStatus()),
			() -> assertEquals(entity.getCreatedAt(), domain.getCreatedAt()),
			() -> assertEquals(entity.getUpdatedAt(), domain.getUpdatedAt())
		);
	}

	@Test
	@DisplayName("도메인 -> 엔티티 변환 성공")
	void toEntity_success() {
		LocalDateTime now = LocalDateTime.now();
		QnaPost domain = new QnaPost(
			99L,
			QnaType.ANSWER,
			1L,
			20L,
			"엔티티 매핑 테스트",
			QnaStatus.ACTIVE,
			now,
			null
		);

		QnaPostJpaEntity entity = QnaPostMapper.toEntity(domain);

		assertAll(
			() -> assertNull(entity.getPostId()), // builder에서 postId 세팅하지 않음
			() -> assertEquals(domain.getType(), entity.getType()),
			() -> assertEquals(domain.getParentId(), entity.getParentId()),
			() -> assertEquals(domain.getMemberId(), entity.getMemberId()),
			() -> assertEquals(domain.getContent(), entity.getContent()),
			() -> assertEquals(domain.getStatus(), entity.getStatus()),
			() -> assertEquals(domain.getCreatedAt(), entity.getCreatedAt()),
			() -> assertNull(entity.getUpdatedAt()) // builder에서 updatedAt 세팅하지 않음
		);
	}

	@Test
	@DisplayName("null 입력 시 NPE 발생")
	void nullInput_throws() {
		assertThrows(NullPointerException.class,
			() -> QnaPostMapper.toDomain(null));

		assertThrows(NullPointerException.class,
			() -> QnaPostMapper.toEntity(null));
	}
}