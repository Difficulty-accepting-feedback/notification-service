package com.grow.notification_service.note.application.port;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface MemberPort {

	/** 멤버 ID ->닉네임 (단건) */
	String getMemberName(Long memberId);

	/** 닉네임 -> 멤버 ID */
	ResolveResult resolveByNickname(String nickname);

	record ResolveResult(Long memberId, String nickname) {
		public ResolveResult {
			Objects.requireNonNull(memberId, "memberId");
			Objects.requireNonNull(nickname, "nickname");
		}
	}
}