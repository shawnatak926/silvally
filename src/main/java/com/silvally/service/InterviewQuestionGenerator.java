package com.silvally.service;

import java.util.List;

/** AI 어댑터를 주입할 수 있는 질문 생성 경계. 기본 구현은 키가 필요 없는 mock입니다. */
public interface InterviewQuestionGenerator {
    record Context(String kind, String subjectCode, List<String> recentActivities, String baseQuestion) { }
    record Generated(String question, String source) { }
    Generated generate(Context context) throws Exception;
}
