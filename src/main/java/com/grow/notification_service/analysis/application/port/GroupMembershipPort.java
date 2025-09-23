package com.grow.notification_service.analysis.application.port;

import java.time.LocalDateTime;
import java.util.List;

public interface GroupMembershipPort {
	List<GroupSimpleResponse> getMyJoinedGroups(Long memberId, String category);

	record GroupSimpleResponse(
		Long groupId,
		String groupName,
		String role,
		LocalDateTime joinedAt,
		String skillTag
	) {}
}