package com.grow.notification_service.chatbot.infra.persistence.enums;

public enum QNA {
	// 인텔리제이 자동생성 예시
	QUESTION_1("What is your name?", "My name is Chatbot."),
	QUESTION_2("How can I help you?", "You can ask me anything about our services."),
	QUESTION_3("What are your working hours?", "We are available 24/7."),
	QUESTION_4("Where are you located?", "We operate online, so we don't have a physical location."),
	QUESTION_5("How do I contact support?", "You can contact support via email or chat.");

	private final String question;
	private final String answer;

	QNA(String question, String answer) {
		this.question = question;
		this.answer = answer;
	}

	public String getQuestion() {
		return question;
	}

	public String getAnswer() {
		return answer;
	}
}