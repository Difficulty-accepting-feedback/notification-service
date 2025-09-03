package com.grow.notification_service.qna.application.port;

public interface AuthorityCheckerPort {
	boolean isAdmin(Long memberId);
}