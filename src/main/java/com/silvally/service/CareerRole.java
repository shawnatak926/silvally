package com.silvally.service;

import java.util.Map;
import java.util.Set;

/**
 * 직무 카탈로그 한 건.
 *
 * @param order 추천 동점 처리에 쓰는 고정 순번(작을수록 앞)
 * @param stats 직무 적합 능력치와 가중치
 * @param tags  비포스쿨 활동 태그와 맞춰 보는 값
 */
public record CareerRole(String code, String label, int order, Map<String, Integer> stats, Set<String> tags) {
}
