package com.silvally.service;

/**
 * 멱등 처리 대상 커맨드의 식별 정보.
 * kind·subjectCode·payload로 요청 해시를 만들고, 저장된 응답은 responseType으로 복원한다.
 */
public record CommandSpec<T>(String kind, String subjectCode, Object payload, Class<T> responseType) {
}
