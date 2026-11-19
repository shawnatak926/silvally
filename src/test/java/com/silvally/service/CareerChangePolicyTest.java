package com.silvally.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CareerChangePolicyTest {

    private final CareerChangePolicy policy = new CareerChangePolicy();

    @Test
    @DisplayName("현재 직무는 제외하고 최초 추천이거나 능력치 점수가 기준 이상인 직무만 순번 순으로 돌려준다")
    void eligibleRoles_topOrEnoughStatScore() {
        List<CareerRecommender.Scored> scored = List.of(
                scored("D", 4, "35.00"),
                scored("C", 3, "10.00"),
                scored("B", 2, "10.00"),
                scored("A", 1, "50.00"));

        List<String> eligible = policy.eligibleRoles("A", Set.of("B"), scored, 0);

        // A는 현재 직무, B는 최초 추천, C는 점수 부족, D는 점수 충족
        assertThat(eligible).containsExactly("B", "D");
    }

    @Test
    @DisplayName("점수 기준은 경계값 30을 포함한다")
    void eligibleRoles_boundaryScoreIncluded() {
        List<CareerRecommender.Scored> scored = List.of(scored("A", 1, "29.99"), scored("B", 2, "30.00"));

        assertThat(policy.eligibleRoles("X", Set.of(), scored, 0)).containsExactly("B");
    }

    @Test
    @DisplayName("변경 횟수를 모두 쓰면 변경할 수 있는 직무가 없다")
    void eligibleRoles_changeLimitReached_empty() {
        List<CareerRecommender.Scored> scored = List.of(scored("A", 1, "90.00"));

        assertThat(policy.eligibleRoles("X", Set.of("A"), scored, CareerChangePolicy.MAX_CHANGES)).isEmpty();
        assertThat(policy.eligibleRoles("X", Set.of("A"), scored, CareerChangePolicy.MAX_CHANGES - 1))
                .containsExactly("A");
    }

    private static CareerRecommender.Scored scored(String code, int order, String statScore) {
        CareerRole role = new CareerRole(code, code, order, Map.of("P", 1), Set.of());
        BigDecimal score = new BigDecimal(statScore);
        return new CareerRecommender.Scored(role, score, score, 0, List.of(), List.of());
    }
}
