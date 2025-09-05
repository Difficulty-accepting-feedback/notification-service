package com.grow.notification_service.notice.infra.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.grow.notification_service.notice.domain.model.Notice;
import com.grow.notification_service.notice.domain.repository.NoticeRepository;
import com.grow.notification_service.notice.infra.persistence.mapper.NoticeMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepository {

	private final NoticeJpaRepository jpa;

	/**
	 * 공지사항 저장
	 * @param notice 공지사항 도메인 객체
	 * @return 저장된 공지사항 도메인 객체
	 */
	@Override
	public Notice save(Notice notice) {
		return NoticeMapper.toDomain(jpa.save(NoticeMapper.toEntity(notice)));
	}

	/**
	 * 공지사항 ID로 조회
	 * @param id 공지사항 ID
	 * @return 조회된 공지사항 도메인 객체 (없으면 빈 Optional)
	 */
	@Override
	public Optional<Notice> findById(Long id) {
		return jpa.findById(id).map(NoticeMapper::toDomain);
	}

	/**
	 * 공지사항 ID로 삭제
	 * @param id 공지사항 ID
	 */
	@Override
	public void deleteById(Long id) {
		jpa.deleteById(id);
	}

	/**
	 * 공지 고정, 최신순으로 페이징 조회
	 * @param pageable 페이징 정보
	 * @return 공지사항 도메인 객체의 페이징 결과
	 */
	@Override
	public Page<Notice> findPinnedFirstOrderByCreatedAtDesc(Pageable pageable) {
		return jpa.findAllByOrderByIsPinnedDescCreatedAtDesc(pageable)
			.map(NoticeMapper::toDomain);
	}
}