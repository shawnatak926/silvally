package com.silvally.service;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 희망 직무 확정·변경 규칙. 상태를 바꾸지 않는다.
 *
 * <ul>
 *     <li>처음 확정은 적합도순으로 나열한 전체 직무 중에서 자유롭게 고른다.</li>
 *     <li>변경은 재학 중 최대 {@value #MAX_CHANGES}번까지 할 수 있다.</li>
 *     <li>변경 대상은 현재 직무가 아니면서, 최초 추천 상위 직무이거나 현재 능력치 점수가
 *     {@value #CHANGE_MIN_STAT_SCORE} 이상인 직무로 제한한다.</li>
 * </ul>
 */
@Component
public class CareerChangePolicy {
    public static final int MAX_CHANGES = 2;
    public static final int CHANGE_MIN_STAT_SCORE = 30;

    /** 변경할 수 있는 직무 코드를 고정 순번 순으로 돌려준다. 횟수를 다 썼으면 비어 있다. */
    public List<String> eligibleRoles(String currentRole, Set<String> initialTopRoles,
                                      List<CareerRecommender.Scored> scoredNow, int changesUsed) {
        if (changesUsed >= MAX_CHANGES) {
            return List.of();
        }
        return scoredNow.stream()
                .filter(scored -> !scored.role().code().equals(currentRole))
                .filter(scored -> initialTopRoles.contains(scored.role().code())
                        || scored.statScore().intValue() >= CHANGE_MIN_STAT_SCORE)
                .sorted(Comparator.comparingInt(scored -> scored.role().order()))
                .map(scored -> scored.role().code())
                .toList();
    }
}
