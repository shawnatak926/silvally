package com.silvally.service;

import com.silvally.dto.response.CareerPreferenceView.Recommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CareerRecommenderTest {

    private final CareerRecommender recommender = new CareerRecommender();

    @Test
    @DisplayName("능력치 가중 평균과 활동 태그 일치 수로 점수를 매겨 전체 직무를 적합도순으로 나열한다")
    void recommend_scoresAndRanksAllRoles() {
        List<CareerRole> roles = List.of(
                role("A", 1, Map.of("P", 3, "B", 1), "tagA"),
                role("B", 2, Map.of("P", 1), "tagB"),
                role("C", 3, Map.of("B", 1), "tagC"),
                role("D", 4, Map.of("F", 1), "tagD"));
        Map<String, Integer> stats = Map.of("P", 40, "B", 20, "F", 10);
        List<CareerRecommender.Activity> activities = List.of(
                activity(1, "BS01", "LAB", "tagB", "tagC"));

        List<Recommendation> result = recommender.recommend(roles, stats, activities);

        assertThat(result).extracting(Recommendation::roleCode).containsExactly("B", "A", "C", "D");
        assertThat(result).extracting(Recommendation::rank).containsExactly(1, 2, 3, 4);
        Recommendation first = result.get(0);
        // B: 능력치 P 40 + 활동 태그 1개 4점
        assertThat(first.score()).isEqualByComparingTo("44.00");
        assertThat(first.statReasons()).singleElement().satisfies(reason -> {
            assertThat(reason.statCode()).isEqualTo("P");
            assertThat(reason.value()).isEqualTo(40);
            assertThat(reason.contribution()).isEqualByComparingTo("40.00");
        });
        assertThat(first.activityReasons()).singleElement().satisfies(reason -> {
            assertThat(reason.eventCode()).isEqualTo("BS01");
            assertThat(reason.choiceCode()).isEqualTo("LAB");
            assertThat(reason.tag()).isEqualTo("tagB");
            assertThat(reason.contribution()).isEqualByComparingTo("4.00");
        });
    }

    @Test
    @DisplayName("총점이 같으면 태그 일치 수가 많은 직무가 앞선다")
    void recommend_tieOnScore_moreTagMatchesWins() {
        // X: 능력치 20 + 태그 1개 4점 = 24, Y: 능력치 24 + 태그 0개 = 24 (순번은 Y가 앞)
        List<CareerRole> roles = List.of(
                role("Y", 1, Map.of("A", 1)),
                role("X", 2, Map.of("P", 1), "tagX"));
        Map<String, Integer> stats = Map.of("P", 20, "A", 24);

        List<Recommendation> result = recommender.recommend(roles, stats,
                List.of(activity(1, "BS01", "LAB", "tagX")));

        assertThat(result).extracting(Recommendation::roleCode).containsExactly("X", "Y");
        assertThat(result.get(0).score()).isEqualByComparingTo(result.get(1).score());
    }

    @Test
    @DisplayName("총점과 태그 일치 수가 모두 같으면 직무 고정 순번이 빠른 직무가 앞선다")
    void recommend_fullTie_fixedOrderWins() {
        List<CareerRole> roles = List.of(
                role("LATE", 9, Map.of("P", 1), "tag"),
                role("EARLY", 2, Map.of("P", 1), "tag"),
                role("MIDDLE", 5, Map.of("P", 1), "tag"));
        Map<String, Integer> stats = Map.of("P", 30);

        List<Recommendation> withActivity = recommender.recommend(roles, stats,
                List.of(activity(1, "BS01", "LAB", "tag")));
        List<Recommendation> noActivity = recommender.recommend(roles, stats, List.of());

        assertThat(withActivity).extracting(Recommendation::roleCode).containsExactly("EARLY", "MIDDLE", "LATE");
        assertThat(noActivity).extracting(Recommendation::roleCode).containsExactly("EARLY", "MIDDLE", "LATE");
    }

    @Test
    @DisplayName("한 활동의 태그가 여러 개 일치하면 일치 수만큼 가산하고 일치하지 않는 활동은 이유에 넣지 않는다")
    void score_multipleTagsMatched_andUnmatchedActivitiesIgnored() {
        CareerRole role = role("R", 1, Map.of("P", 1), "web", "android", "ios");

        CareerRecommender.Scored scored = recommender.score(role, Map.of("P", 10), List.of(
                activity(5, "BS05", "WEB_MOBILE", "web", "android", "ios"),
                activity(3, "BS03", "LOOPS", "algorithm")));

        assertThat(scored.tagMatches()).isEqualTo(3);
        assertThat(scored.score()).isEqualByComparingTo("22.00");
        assertThat(scored.activityReasons()).singleElement().satisfies(reason -> {
            assertThat(reason.day()).isEqualTo(5);
            assertThat(reason.tag()).isEqualTo("android,ios,web");
            assertThat(reason.contribution()).isEqualByComparingTo("12.00");
        });
    }

    @Test
    @DisplayName("능력치 기여도는 가중치에 비례하고 소수 둘째 자리까지 반올림한다")
    void score_statContributionWeighted() {
        CareerRole role = role("R", 1, Map.of("F", 2, "C", 1));

        CareerRecommender.Scored scored = recommender.score(role, Map.of("F", 10, "C", 5), List.of());

        assertThat(scored.statReasons()).extracting(reason -> reason.contribution().toPlainString())
                .containsExactlyInAnyOrder("6.67", "1.67");
        assertThat(scored.statScore()).isEqualByComparingTo("8.34");
    }

    private static CareerRole role(String code, int order, Map<String, Integer> stats, String... tags) {
        return new CareerRole(code, code + " 직무", order, stats, Set.of(tags));
    }

    private static CareerRecommender.Activity activity(int day, String event, String choice, String... tags) {
        return new CareerRecommender.Activity(day, event, choice, choice + " 활동", Set.of(tags));
    }
}
