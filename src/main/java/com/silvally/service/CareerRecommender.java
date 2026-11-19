package com.silvally.service;

import com.silvally.dto.response.CareerPreferenceView.ActivityReason;
import com.silvally.dto.response.CareerPreferenceView.Recommendation;
import com.silvally.dto.response.CareerPreferenceView.StatReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 비포스쿨 활동과 능력치로 직무 추천 순위를 계산하는 순수 규칙. 상태를 바꾸지 않는다.
 *
 * <ul>
 *     <li>능력치 점수 = 직무 적합 능력치의 가중 평균(0~100). 능력치별 기여도가 추천 이유가 된다.</li>
 *     <li>활동 점수 = 활동 태그와 직무 태그가 일치한 개수 × {@value #ACTIVITY_POINT}. 일치한 활동이 추천 이유가 된다.</li>
 *     <li>순위 = 총점 높은 순, 같으면 태그 일치 수 많은 순, 그래도 같으면 직무 고정 순번 빠른 순.</li>
 * </ul>
 */
@Component
public class CareerRecommender {
    public static final int ACTIVITY_POINT = 4;
    public static final int RECOMMEND_LIMIT = 3;

    /** 비포스쿨에서 한 활동의 근거. */
    public record Activity(int day, String eventCode, String choiceCode, String label, Set<String> tags) {
    }

    /** 직무 한 개의 계산 결과. */
    public record Scored(CareerRole role, BigDecimal score, BigDecimal statScore, int tagMatches,
                         List<StatReason> statReasons, List<ActivityReason> activityReasons) {
    }

    public List<Recommendation> recommend(List<CareerRole> roles, Map<String, Integer> stats,
                                          List<Activity> activities) {
        List<Scored> ranked = scoreAll(roles, stats, activities);
        List<Recommendation> result = new ArrayList<>();
        for (int i = 0; i < Math.min(RECOMMEND_LIMIT, ranked.size()); i++) {
            Scored scored = ranked.get(i);
            result.add(new Recommendation(i + 1, scored.role().code(), scored.role().label(), scored.score(),
                    scored.statReasons(), scored.activityReasons()));
        }
        return result;
    }

    /** 모든 직무를 순위 순으로 돌려준다. */
    public List<Scored> scoreAll(List<CareerRole> roles, Map<String, Integer> stats, List<Activity> activities) {
        return roles.stream()
                .map(role -> score(role, stats, activities))
                .sorted(Comparator.comparing(Scored::score).reversed()
                        .thenComparing(Comparator.comparingInt(Scored::tagMatches).reversed())
                        .thenComparingInt(scored -> scored.role().order()))
                .toList();
    }

    public Scored score(CareerRole role, Map<String, Integer> stats, List<Activity> activities) {
        int weightSum = role.stats().values().stream().mapToInt(Integer::intValue).sum();
        List<StatReason> statReasons = new ArrayList<>();
        BigDecimal statScore = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : role.stats().entrySet()) {
            int value = stats.getOrDefault(entry.getKey(), 0);
            BigDecimal contribution = BigDecimal.valueOf((long) entry.getValue() * value)
                    .divide(BigDecimal.valueOf(weightSum), 2, RoundingMode.HALF_UP);
            statReasons.add(new StatReason(entry.getKey(), value, contribution));
            statScore = statScore.add(contribution);
        }

        List<ActivityReason> activityReasons = new ArrayList<>();
        int tagMatches = 0;
        BigDecimal activityScore = BigDecimal.ZERO;
        for (Activity activity : activities) {
            Set<String> matched = new TreeSet<>(new LinkedHashSet<>(activity.tags()));
            matched.retainAll(role.tags());
            if (matched.isEmpty()) {
                continue;
            }
            BigDecimal contribution = BigDecimal.valueOf((long) matched.size() * ACTIVITY_POINT)
                    .setScale(2, RoundingMode.HALF_UP);
            activityReasons.add(new ActivityReason(activity.day(), activity.eventCode(), activity.choiceCode(),
                    activity.label(), String.join(",", matched), contribution));
            tagMatches += matched.size();
            activityScore = activityScore.add(contribution);
        }
        return new Scored(role, statScore.add(activityScore), statScore, tagMatches, statReasons, activityReasons);
    }
}
