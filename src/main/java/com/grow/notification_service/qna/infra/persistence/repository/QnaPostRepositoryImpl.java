package com.grow.notification_service.qna.infra.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaType;
import com.grow.notification_service.qna.domain.repository.QnaPostRepository;
import com.grow.notification_service.qna.infra.persistence.entity.QnaPostJpaEntity;
import com.grow.notification_service.qna.infra.persistence.mapper.QnaPostMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaPostRepositoryImpl implements QnaPostRepository {

	private final QnaPostJpaRepository jpa;

	/**
	 * QnaPost 저장
	 * @param post 저장할 QnaPost
	 * @return 저장된 QnaPost의 ID
	 */
	@Override
	@Transactional
	public Long save(QnaPost post) {
		QnaPostJpaEntity saved = jpa.save(QnaPostMapper.toEntity(post)); // id null=insert, not null=merge
		return saved.getPostId();
	}

	/**
	 * ID로 QnaPost 조회
	 * @param id 조회할 QnaPost ID
	 * @return 조회된 QnaPost (없으면 Optional.empty())
	 */
	@Override
	public Optional<QnaPost> findById(Long id) {
		return jpa.findById(id).map(QnaPostMapper::toDomain);
	}

	/**
	 * 질문 목록 조회 (페이징)
	 * @param pageable 페이징 정보
	 * @return 질문 목록 페이지
	 */
	@Override
	public Page<QnaPost> findQuestions(Pageable pageable) {
		Page<QnaPostJpaEntity> page = jpa.findByTypeOrderByCreatedAtDesc(QnaType.QUESTION, pageable);
		List<QnaPost> content = page.getContent().stream().map(QnaPostMapper::toDomain).toList();
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	/**
	 * ID로 존재 여부 확인
	 * @param id 확인할 QnaPost ID
	 * @return 존재하면 true, 없으면 false
	 */
	@Override
	public boolean existsById(Long id) {
		return jpa.existsByPostId(id);
	}
}