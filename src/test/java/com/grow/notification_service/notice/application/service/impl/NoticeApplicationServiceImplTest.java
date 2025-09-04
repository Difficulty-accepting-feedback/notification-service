package com.grow.notification_service.notice.application.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.grow.notification_service.global.exception.NoticeException;
import com.grow.notification_service.notice.domain.model.Notice;
import com.grow.notification_service.notice.domain.repository.NoticeRepository;
import com.grow.notification_service.qna.application.port.AuthorityCheckerPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NoticeApplicationServiceImplTest {

	@Mock private NoticeRepository noticeRepository;
	@Mock private AuthorityCheckerPort authorityCheckerPort;
	@Mock private Clock clock;

	@InjectMocks
	private NoticeApplicationServiceImpl service;

	private final Instant fixedInstant = Instant.parse("2025-09-04T12:34:56Z");
	private final ZoneId zone = ZoneId.of("UTC");
	private LocalDateTime fixedNow;

	@BeforeEach
	void setUp() {
		lenient().when(clock.instant()).thenReturn(fixedInstant);
		lenient().when(clock.getZone()).thenReturn(zone);
		fixedNow = LocalDateTime.ofInstant(fixedInstant, zone);
	}

	private Notice notice(Long id, String title, String content, boolean pinned, LocalDateTime createdAt) {
		return new Notice(id, title, content, pinned, createdAt);
	}

	// --- create ---
	@Test
	@DisplayName("관리자는 공지를 생성할 수 있다")
	void create_success() {
		Long adminId = 1L;
		when(authorityCheckerPort.isAdmin(adminId)).thenReturn(true);

		// save 시점에 ID가 부여된 도메인을 반환(레포지토리 구현을 가정한 스텁)
		when(noticeRepository.save(any())).thenAnswer(inv -> {
			Notice n = inv.getArgument(0, Notice.class);
			return notice(10L, n.getTitle(), n.getContent(), n.isPinned(), n.getCreatedAt());
		});

		Notice saved = service.create(adminId, "제목", "내용", true);

		assertThat(saved.getNoticeId()).isEqualTo(10L);
		assertThat(saved.getTitle()).isEqualTo("제목");
		assertThat(saved.getContent()).isEqualTo("내용");
		assertThat(saved.isPinned()).isTrue();
		assertThat(saved.getCreatedAt()).isEqualTo(fixedNow);

		verify(authorityCheckerPort).isAdmin(adminId);
		verify(noticeRepository).save(any(Notice.class));
	}

	@Test
	@DisplayName("비관리자는 공지를 생성할 수 없다")
	void create_forbidden() {
		when(authorityCheckerPort.isAdmin(2L)).thenReturn(false);
		assertThatThrownBy(() -> service.create(2L, "a", "b", false))
			.isInstanceOf(NoticeException.class);
		verifyNoMoreInteractions(noticeRepository);
	}

	// --- edit ---
	@Test
	@DisplayName("관리자는 공지의 제목/내용을 수정할 수 있다")
	void edit_success() {
		Long adminId = 1L;
		when(authorityCheckerPort.isAdmin(adminId)).thenReturn(true);

		Notice origin = notice(11L, "old", "oldC", false, fixedNow);
		when(noticeRepository.findById(11L)).thenReturn(Optional.of(origin));
		// save는 전달된 도메인을 그대로 반환해도 무방
		when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> inv.getArgument(0));

		Notice updated = service.edit(adminId, 11L, "new", "newC");

		assertThat(updated.getNoticeId()).isEqualTo(11L);
		assertThat(updated.getTitle()).isEqualTo("new");
		assertThat(updated.getContent()).isEqualTo("newC");
		assertThat(updated.isPinned()).isFalse();

		verify(noticeRepository).findById(11L);
		verify(noticeRepository).save(any(Notice.class));
	}

	@Test
	@DisplayName("수정 대상 공지가 없으면 NOT_FOUND 예외")
	void edit_notFound() {
		when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
		when(noticeRepository.findById(999L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.edit(1L, 999L, "t", "c"))
			.isInstanceOf(NoticeException.class);
	}

	@Test
	@DisplayName("비관리자는 공지를 수정할 수 없다")
	void edit_forbidden() {
		when(authorityCheckerPort.isAdmin(1L)).thenReturn(false);
		assertThatThrownBy(() -> service.edit(1L, 11L, "t", "c"))
			.isInstanceOf(NoticeException.class);
		verify(noticeRepository, never()).findById(anyLong());
	}

	// --- setPinned ---
	@Test
	@DisplayName("관리자는 공지 고정/해제를 설정할 수 있다")
	void setPinned_success() {
		when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);

		Notice n = notice(20L, "t", "c", false, fixedNow);
		when(noticeRepository.findById(20L)).thenReturn(Optional.of(n));
		when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> inv.getArgument(0));

		Notice pinned = service.setPinned(1L, 20L, true);

		assertThat(pinned.isPinned()).isTrue();
		verify(noticeRepository).findById(20L);
		verify(noticeRepository).save(any(Notice.class));
	}

	@Test
	@DisplayName("고정 대상 공지가 없으면 NOT_FOUND 예외")
	void setPinned_notFound() {
		when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);
		when(noticeRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.setPinned(1L, 404L, true))
			.isInstanceOf(NoticeException.class);
	}

	@Test
	@DisplayName("비관리자는 공지 고정/해제를 할 수 없다")
	void setPinned_forbidden() {
		when(authorityCheckerPort.isAdmin(1L)).thenReturn(false);
		assertThatThrownBy(() -> service.setPinned(1L, 1L, true))
			.isInstanceOf(NoticeException.class);
		verify(noticeRepository, never()).findById(anyLong());
	}

	// --- delete ---
	@Test
	@DisplayName("관리자는 공지를 삭제할 수 있다")
	void delete_success() {
		when(authorityCheckerPort.isAdmin(1L)).thenReturn(true);

		service.delete(1L, 33L);

		verify(noticeRepository).deleteById(33L);
	}

	@Test
	@DisplayName("비관리자는 공지를 삭제할 수 없다")
	void delete_forbidden() {
		when(authorityCheckerPort.isAdmin(1L)).thenReturn(false);
		assertThatThrownBy(() -> service.delete(1L, 33L))
			.isInstanceOf(NoticeException.class);
		verify(noticeRepository, never()).deleteById(anyLong());
	}

	// --- get ---
	@Test
	@DisplayName("공지 단건 조회")
	void get_success() {
		Notice n = notice(55L, "t", "c", false, fixedNow);
		when(noticeRepository.findById(55L)).thenReturn(Optional.of(n));

		Notice found = service.get(55L);

		assertThat(found.getNoticeId()).isEqualTo(55L);
		assertThat(found.getTitle()).isEqualTo("t");
		verify(noticeRepository).findById(55L);
	}

	@Test
	@DisplayName("공지 단건 조회 - 없으면 NOT_FOUND")
	void get_notFound() {
		when(noticeRepository.findById(777L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.get(777L))
			.isInstanceOf(NoticeException.class);
	}

	// --- getPage ---
	@Test
	@DisplayName("공지 페이지 조회 - 고정 우선, 최신순(레포지토리 계약을 그대로 반환)")
	void getPage_success() {
		Notice pinned = notice(1L, "p", "pc", true, fixedNow.plusMinutes(1));
		Notice normal = notice(2L, "n", "nc", false, fixedNow);

		PageRequest pr = PageRequest.of(0, 10);
		when(noticeRepository.findPinnedFirstOrderByCreatedAtDesc(pr))
			.thenReturn(new PageImpl<>(List.of(pinned, normal), pr, 2));

		Page<Notice> page = service.getPage(pr);

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent()).containsExactly(pinned, normal);
		verify(noticeRepository).findPinnedFirstOrderByCreatedAtDesc(pr);
	}

	@Nested
	@DisplayName("경계/예외 케이스 보조 검증")
	class EdgeCases {
		@Test
		@DisplayName("create에서 Clock 기반 생성시각이 정확히 주입된다")
		void create_usesClockNow() {
			when(authorityCheckerPort.isAdmin(99L)).thenReturn(true);
			when(noticeRepository.save(any())).thenAnswer(inv -> {
				Notice n = inv.getArgument(0, Notice.class);
				return notice(100L, n.getTitle(), n.getContent(), n.isPinned(), n.getCreatedAt());
			});

			Notice saved = service.create(99L, "t", "c", false);
			assertThat(saved.getCreatedAt()).isEqualTo(fixedNow);
		}
	}
}