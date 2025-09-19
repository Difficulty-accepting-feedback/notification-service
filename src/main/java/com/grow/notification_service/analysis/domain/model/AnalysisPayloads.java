package com.grow.notification_service.analysis.domain.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class AnalysisPayloads {
	public static Analysis latest(Long memberId, Long categoryId, ObjectMapper om,
		List<Long> usedQuizIds, List<Map<String,String>> focus,
		List<String> future, String signature, String sessionId){
		try {
			Map<String,Object> out = new java.util.LinkedHashMap<>();
			out.put("memberId", memberId);
			out.put("categoryId", categoryId);
			out.put("source", "latest");
			out.put("signature", signature);
			out.put("sessionId", sessionId);
			out.put("usedQuizIds", usedQuizIds);
			out.put("focusConcepts", focus);
			out.put("futureConcepts", future);
			return new Analysis(memberId, categoryId, om.writeValueAsString(out));
		} catch(Exception e){ throw new RuntimeException(e); }
	}
}