package com.grow.notification_service.qna.infra.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.qna.domain.model.QnaPost;
import com.grow.notification_service.qna.domain.model.enums.QnaStatus;
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
		QnaPostJpaEntity saved = jpa.save(QnaPostMapper.toEntity(post));
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
	 * 유형별 QnaPost 조회 (페이징)
	 * @param type QnaType (QUESTION or ANSWER)
	 * @param pageable 페이징 정보
	 * @return QnaPost 목록 페이지
	 */
	@Override
	public Page<QnaPost> findByType(QnaType type, Pageable pageable) {
		Page<QnaPostJpaEntity> page = jpa.findByTypeOrderByCreatedAtDesc(type, pageable);
		List<QnaPost> content = page.getContent().stream().map(QnaPostMapper::toDomain).toList();
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	/**
	 * 내가 작성한 질문 목록 조회 (페이징)
	 * @param memberId 회원 ID
	 * @param pageable 페이징 정보
	 * @return 내가 작성한 질문 목록 페이지
	 */
	@Override
	public Page<QnaPost> findMyQuestions(Long memberId, Pageable pageable) {
		Page<QnaPostJpaEntity> page =
			jpa.findByTypeAndMemberIdOrderByCreatedAtDesc(QnaType.QUESTION, memberId, pageable);
		List<QnaPost> content = page.getContent().stream().map(QnaPostMapper::toDomain).toList();
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	/**
	 * 부모 ID로 자식 답변들 조회
	 * @param parentId 부모 질문 ID
	 * @return 자식 답변 목록
	 */
	@Override
	public List<QnaPost> findChildren(Long parentId) {
		return jpa.findByParentIdOrderByCreatedAtAsc(parentId)
			.stream()
			.map(QnaPostMapper::toDomain)
			.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * 루트 질문 트리 조회 (플랫 리스트)
	 * 루트 질문과 그에 속한 모든 답변들을 플랫 리스트 형태로 조회합니다.
	 * @param rootId 루트 질문 ID
	 * @return 루트 질문과 그에 속한 모든 답변들의 플랫 리스트
	 */
	@Override
	public List<QnaPost> findTreeFlat(Long rootId) {
		List<QnaPostJpaEntity> rows = jpa.findTreeByRootId(rootId);
		return rows.stream().map(QnaPostMapper::toDomain).toList();
	}

	/**
	 * 루트 질문 목록 조회 (페이징)
	 * @param pageable 페이징 정보
	 * @return 루트 질문 목록 페이지
	 */
	@Override
	public Page<QnaPost> findRootQuestions(Pageable pageable) {
		Page<QnaPostJpaEntity> page =
			jpa.findByTypeAndParentIdIsNullOrderByCreatedAtDesc(QnaType.QUESTION, pageable);
		List<QnaPost> content = page.getContent().stream().map(QnaPostMapper::toDomain).toList();
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	/**
	 * 내가 작성한 루트 질문 목록 조회 (페이징)
	 * @param memberId 회원 ID
	 * @param pageable 페이징 정보
	 * @return 내가 작성한 루트 질문 목록 페이지
	 */
	@Override
	public Page<QnaPost> findMyRootQuestions(Long memberId, Pageable pageable) {
		Page<QnaPostJpaEntity> page =
			jpa.findByTypeAndMemberIdAndParentIdIsNullOrderByCreatedAtDesc(
				QnaType.QUESTION, memberId, pageable
			);
		List<QnaPost> content = page.getContent().stream().map(QnaPostMapper::toDomain).toList();
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	/**
	 * 시작 ID로부터 루트 질문의 상태 갱신
	 * @param startId 시작 QnaPost ID (질문 또는 답변)
	 * @param status 새 상태
	 * @return 갱신된 행 수 (0 또는 1)
	 */
	@Override
	@Transactional
	public int updateRootStatusFrom(Long startId, QnaStatus status) {
		Long rootId = jpa.findRootIdFromAny(startId);
		return jpa.updateStatusById(rootId, status.name());
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