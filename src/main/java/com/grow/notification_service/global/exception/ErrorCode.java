package com.grow.notification_service.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	// 쪽지
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "404-0", "member.not.found"),
	MEMBER_RESOLVE_FAILED(HttpStatus.BAD_REQUEST, "400-1", "member.resolve.failed"),
	MEMBER_RESOLVE_EMPTY(HttpStatus.BAD_REQUEST, "400-2", "member.resolve.empty"),

	// Q&A
	MEMBER_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-0", "member.check.failed"),
	NO_PERMISSION_TO_WRITE_ANSWER(HttpStatus.FORBIDDEN, "403-0", "no.permission.to.write.answer"),
	QNA_NOT_FOUND(HttpStatus.NOT_FOUND, "404-1", "qna.not.found"),
	INVALID_QNA_PARENT(HttpStatus.BAD_REQUEST, "400-3", "invalid.qna.parent"),
	QNA_FORBIDDEN(HttpStatus.FORBIDDEN, "403-1", "qna.forbidden"),

	// 공지사항
	NO_PERMISSION_TO_WRITE_NOTICE(HttpStatus.FORBIDDEN, "403-2", "no.permission.to.write.notice"),
	NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "404-2", "notice.not.found"),

	// 퀴즈
	MEMBER_RESULT_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-1", "member.result.fetch.failed"),
	INVALID_SKILL_TAG(HttpStatus.BAD_REQUEST, "400-4", "invalid.skill.tag"),
	UNSUPPORTED_MODE(HttpStatus.BAD_REQUEST, "400-5", "unsupported.mode"),
	QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "404-3", "quiz.not.found"),
	CATEGORY_MISMATCH(HttpStatus.BAD_REQUEST, "400-6", "category.mismatch"),
	NOT_ENOUGH_HISTORY(HttpStatus.BAD_REQUEST, "400-7", "not.enough.history"),

	// 분석
	ANALYSIS_INPUT_SERIALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-2", "analysis.input.serialize.failed"),
	ANALYSIS_SUMMARY_PROMPT_BUILD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-3", "analysis.summary.prompt.build.failed"),
	ANALYSIS_KEYWORDS_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-4", "analysis.keywords.parse.failed"),
	ANALYSIS_FOCUS_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-5", "analysis.focus.parse.failed"),
	ANALYSIS_FUTURE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-6", "analysis.future.parse.failed"),
	ANALYSIS_OUTPUT_SERIALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-7", "analysis.output.serialize.failed"),
	ANALYSIS_LLM_CALL_FAILED(HttpStatus.BAD_GATEWAY, "502-0", "analysis.llm.call.failed"),
	ANALYSIS_QUIZ_GENERATION_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-8", "analysis.quiz.generation.parse.failed"),
	ANALYSIS_REQUIRED_FIELD_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "500-9", "analysis.required.field.missing"),
	ANALYSIS_INVALID_FIELD_VALUE(HttpStatus.INTERNAL_SERVER_ERROR, "500-10", "analysis.invalid.field.value"),
	ANALYSIS_UNEXPECTED_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-11", "analysis.unexpected.failed"),;

	private final HttpStatus status;
	private final String code;
	private final String messageCode;
}