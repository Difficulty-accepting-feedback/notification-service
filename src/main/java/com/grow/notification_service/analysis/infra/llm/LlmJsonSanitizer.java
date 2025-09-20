package com.grow.notification_service.analysis.infra.llm;

public final class LlmJsonSanitizer {
	private LlmJsonSanitizer() {}

	/**
	 * 모델이 코드펜스/서문/후문을 붙이거나 일부 잘랐을 때
	 * 가장 바깥의 배열/객체만 남겨 파싱 안정화.
	 * @param raw 원문
	 * @param expectArray true면 배열 우선([]), false면 객체 우선({})
	 */
	public static String sanitize(String raw, boolean expectArray) {
		if (raw == null) return null;
		String s = raw.trim();

		// 코드펜스 제거
		if (s.startsWith("```")) {
			s = s.replaceFirst("^```json\\s*", "")
				.replaceFirst("^```\\s*", "");
			if (s.endsWith("```")) {
				s = s.substring(0, s.length() - 3);
			}
			s = s.trim();
		}

		int lArr = s.indexOf('['), rArr = s.lastIndexOf(']');
		int lObj = s.indexOf('{'), rObj = s.lastIndexOf('}');

		if (expectArray) {
			if (lArr >= 0 && rArr > lArr) return s.substring(lArr, rArr + 1).trim();
			if (lObj >= 0 && rObj > lObj) return s.substring(lObj, rObj + 1).trim();
		} else {
			if (lObj >= 0 && rObj > lObj) return s.substring(lObj, rObj + 1).trim();
			if (lArr >= 0 && rArr > lArr) return s.substring(lArr, rArr + 1).trim();
		}
		return s;
	}
}