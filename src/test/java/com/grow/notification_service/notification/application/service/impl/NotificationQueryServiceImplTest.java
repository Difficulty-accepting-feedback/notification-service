package com.grow.notification_service.notification.application.service.impl;

import com.grow.notification_service.notification.application.dto.NotificationListItemResponse;
import com.grow.notification_service.notification.infra.persistence.repository.NotificationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceImplTest {

	@Mock
	private NotificationJpaRepository jpa;

	@InjectMocks
	private NotificationQueryServiceImpl service;

	@Nested
	@DisplayName("getPage")
	class GetPage {

		@Test
		@DisplayName("page/size/sort(createdAt DESC) 를 그대로 Repository에 전달하고 결과를 반환한다")
		void forwardsPageableAndReturns() {
			// given
			Long memberId = 7L;
			int page = 2, size = 20;
			@SuppressWarnings("unchecked")
			Page<NotificationListItemResponse> stub =
				new PageImpl<>(List.of(mock(NotificationListItemResponse.class)));
			when(jpa.findPage(eq(memberId), any(Pageable.class))).thenReturn(stub);

			ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

			// when
			Page<NotificationListItemResponse> result = service.getPage(memberId, page, size);

			// then
			verify(jpa, times(1)).findPage(eq(memberId), pageableCaptor.capture());
			Pageable used = pageableCaptor.getValue();

			assertThat(used.getPageNumber()).isEqualTo(page);
			assertThat(used.getPageSize()).isEqualTo(size);

			Sort.Order order = used.getSort().getOrderFor("createdAt");
			assertThat(order).isNotNull();
			assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);

			assertThat(result).isSameAs(stub);
		}
	}

	@Nested
	@DisplayName("unreadCount")
	class UnreadCount {

		@Test
		@DisplayName("읽지 않은 알림 수를 반환한다")
		void returnsCount() {
			// given
			Long memberId = 9L;
			when(jpa.countUnread(memberId)).thenReturn(42L);

			// when
			long count = service.unreadCount(memberId);

			// then
			verify(jpa, times(1)).countUnread(memberId);
			assertThat(count).isEqualTo(42L);
		}
	}

	@Nested
	@DisplayName("markAllRead / markOneRead / deleteOne / deleteOlderThan")
	class Mutations {

		@Test
		@DisplayName("각 메서드가 Repository 값을 그대로 매핑하여 반환한다")
		void mappings() {
			Long memberId = 5L;
			Long id = 77L;
			LocalDateTime before = LocalDateTime.now().minusDays(30);

			when(jpa.markAllRead(memberId)).thenReturn(3);
			when(jpa.markOneRead(memberId, id)).thenReturn(1);
			when(jpa.deleteByNotificationIdAndMemberId(id, memberId)).thenReturn(0);
			when(jpa.deleteOld(memberId, before)).thenReturn(12);

			int all = service.markAllRead(memberId);
			boolean oneRead = service.markOneRead(memberId, id);
			boolean deleted = service.deleteOne(memberId, id);
			int deletedOld = service.deleteOlderThan(memberId, before);

			verify(jpa).markAllRead(memberId);
			verify(jpa).markOneRead(memberId, id);
			verify(jpa).deleteByNotificationIdAndMemberId(id, memberId);
			verify(jpa).deleteOld(memberId, before);

			assertThat(all).isEqualTo(3);
			assertThat(oneRead).isTrue();      // > 0 -> true
			assertThat(deleted).isFalse();     // 0 -> false
			assertThat(deletedOld).isEqualTo(12);
		}
	}

	@Nested
	@DisplayName("topUnread")
	class TopUnread {

		@Test
		@DisplayName("size는 1~10으로 clamp 되고 createdAt DESC 정렬로 0페이지를 조회한다")
		void clampsSizeAndSortsDesc() {
			Long memberId = 11L;

			// stub page contents
			NotificationListItemResponse item1 = mock(NotificationListItemResponse.class);
			NotificationListItemResponse item2 = mock(NotificationListItemResponse.class);

			// size=0 -> clamp to 1
			when(jpa.findTopUnread(eq(memberId), any(Pageable.class)))
				.thenAnswer(inv -> {
					Pageable p = inv.getArgument(1, Pageable.class);
					// validate clamp=1 call
					assertThat(p.getPageNumber()).isEqualTo(0);
					assertThat(p.getPageSize()).isEqualTo(1);
					Sort.Order order = p.getSort().getOrderFor("createdAt");
					assertThat(order).isNotNull();
					assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
					return new PageImpl<>(List.of(item1));
				});

			List<NotificationListItemResponse> r1 = service.topUnread(memberId, 0);
			assertThat(r1).containsExactly(item1);

			// size=50 -> clamp to 10
			when(jpa.findTopUnread(eq(memberId), any(Pageable.class)))
				.thenAnswer(inv -> {
					Pageable p = inv.getArgument(1, Pageable.class);
					// validate clamp=10 call
					assertThat(p.getPageNumber()).isEqualTo(0);
					assertThat(p.getPageSize()).isEqualTo(10);
					Sort.Order order = p.getSort().getOrderFor("createdAt");
					assertThat(order).isNotNull();
					assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
					return new PageImpl<>(List.of(item1, item2));
				});

			List<NotificationListItemResponse> r2 = service.topUnread(memberId, 50);
			assertThat(r2).containsExactly(item1, item2);
		}
	}
}