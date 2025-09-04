package com.grow.notification_service.notice.application.service.impl;

import java.time.Clock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.global.exception.NoticeException;
import com.grow.notification_service.notice.application.service.NoticeApplicationService;
import com.grow.notification_service.notice.domain.model.Notice;
import com.grow.notification_service.notice.domain.repository.NoticeRepository;
import com.grow.notification_service.qna.application.port.AuthorityCheckerPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeApplicationServiceImpl implements NoticeApplicationService {
	//TODO: 외부 API 조회 분리 필요
	private final NoticeRepository noticeRepository;
	private final AuthorityCheckerPort authorityCheckerPort;
	private final Clock clock;

	/**
	 * 공지사항 생성
	 * @param memberId 요청자 회원 ID
	 * @param title 공지 제목
	 * @param content 공지 내용
	 * @param pinned 고정 여부
	 * @return 생성된 공지사항
	 */
	@Override
	@Transactional
	public Notice create(Long memberId, String title, String content, boolean pinned) {
		log.info("[공지][생성][시작] memberId={} title={}", memberId, title);
		assertAdmin(memberId);

		Notice notice = Notice.create(title, content, pinned, clock);
		Notice saved = noticeRepository.save(notice);

		log.info("[공지][생성][완료] noticeId={} title={}", saved.getNoticeId(), saved.getTitle());
		return saved;
	}

	/**
	 * 공지사항 수정
	 * @param memberId 요청자 회원 ID
	 * @param id 공지사항 ID
	 * @param title 수정할 제목
	 * @param content 수정할 내용
	 * @return 수정된 공지사항
	 */
	@Override
	@Transactional
	public Notice edit(Long memberId, Long id, String title, String content) {
		log.info("[공지][수정][시작] memberId={} noticeId={}", memberId, id);
		assertAdmin(memberId);

		Notice n = noticeRepository.findById(id)
			.orElseThrow(() -> new NoticeException(ErrorCode.NOTICE_NOT_FOUND));
		n.edit(title, content);

		Notice updated = noticeRepository.save(n);
		log.info("[공지][수정][완료] noticeId={} title={}", updated.getNoticeId(), updated.getTitle());
		return updated;
	}

	/**
	 * 공지사항 고정/해제
	 * @param memberId 요청자 회원 ID
	 * @param id 공지사항 ID
	 * @param pinned 고정 여부
	 * @return 수정된 공지사항
	 */
	@Override
	@Transactional
	public Notice setPinned(Long memberId, Long id, boolean pinned) {
		log.info("[공지][고정][시작] memberId={} noticeId={} pinned={}", memberId, id, pinned);
		assertAdmin(memberId);

		Notice n = noticeRepository.findById(id)
			.orElseThrow(() -> new NoticeException(ErrorCode.NOTICE_NOT_FOUND));
		n.setPinned(pinned);

		Notice pinnedNotice = noticeRepository.save(n);
		log.info("[공지][고정][완료] noticeId={} pinned={}", pinnedNotice.getNoticeId(), pinnedNotice.isPinned());
		return pinnedNotice;
	}

	/**
	 * 공지사항 삭제
	 * @param memberId 요청자 회원 ID
	 * @param id 공지사항 ID
	 */
	@Override
	@Transactional
	public void delete(Long memberId, Long id) {
		log.info("[공지][삭제][시작] memberId={} noticeId={}", memberId, id);
		assertAdmin(memberId);

		noticeRepository.deleteById(id);
		log.info("[공지][삭제][완료] noticeId={}", id);
	}

	/**
	 * 공지사항 단건 조회
	 * @param id 공지사항 ID
	 * @return 조회된 공지사항
	 */
	@Override
	public Notice get(Long id) {
		log.debug("[공지][조회][시작] noticeId={}", id);
		Notice n = noticeRepository.findById(id)
			.orElseThrow(() -> new NoticeException(ErrorCode.NOTICE_NOT_FOUND));
		log.debug("[공지][조회][완료] noticeId={}", n.getNoticeId());
		return n;
	}

	/**
	 * 공지사항 페이지 조회 (고정된 공지 먼저, 최신순)
	 * @param pageable 페이지 정보
	 * @return 공지사항 페이지
	 */
	@Override
	public Page<Notice> getPage(Pageable pageable) {
		log.debug("[공지][목록][시작] page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
		Page<Notice> result = noticeRepository.findPinnedFirstOrderByCreatedAtDesc(pageable);
		log.debug("[공지][목록][완료] totalElements={}", result.getTotalElements());
		return result;
	}

	/**
	 * 관리자 검증
	 * @param memberId 검증할 회원 ID
	 */
	private void assertAdmin(Long memberId) {
		boolean admin = authorityCheckerPort.isAdmin(memberId);
		log.debug("[공지][권한검증] memberId={} isAdmin={}", memberId, admin);
		if (!admin) throw new NoticeException(ErrorCode.NO_PERMISSION_TO_WRITE_NOTICE);
	}
}