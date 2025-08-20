package com.grow.notification_service.notification.application.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageResponse<T> {
	private final List<T> items;
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;
	private final boolean last;
}