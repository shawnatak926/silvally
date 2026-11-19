package com.silvally.service;

import com.silvally.entity.ExamKind;
import com.silvally.entity.Stats;
import com.silvally.support.TestStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class StatRulesTest {

    @Test
    @DisplayName("초기 능력치는 모든 시드에서 범위 안에 있고 양 끝값도 나온다")
    void initialRoll_staysWithinInclusiveRanges() {
        Map<String, Set<Integer>> seen = new HashMap<>();
        for (long seed = 0; seed < 5000; seed++) {
            StatRules.rollInitial(seed).toMap().forEach((stat, value) -> {
                StatRules.Range range = StatRules.INITIAL_RANGES.get(stat);
                assertThat(value).as(stat).isBetween(range.min(), range.max());
                seen.computeIfAbsent(stat, k -> new HashSet<>()).add(value);
            });
        }
        assertThat(seen.keySet()).containsExactlyInAnyOrderElementsOf(Stats.KEYS);
        StatRules.INITIAL_RANGES.forEach((stat, range) ->
                assertThat(seen.get(stat)).as(stat).contains(range.min(), range.max()));
    }

    @Test
    @DisplayName("같은 회차 시드는 같은 초기 능력치를 재현하고 다른 시드는 다른 값을 낸다")
    void initialRoll_reproducibleBySeed() {
        String seed = UUID.randomUUID().toString();

        assertThat(StatRules.rollInitial(StatRules.statSeed(seed)).toMap())
                .isEqualTo(StatRules.rollInitial(StatRules.statSeed(seed)).toMap());
        Set<Map<String, Integer>> distinct = new HashSet<>();
        IntStream.range(0, 20).forEach(i ->
                distinct.add(StatRules.rollInitial(StatRules.statSeed(UUID.randomUUID().toString())).toMap()));
        assertThat(distinct).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("성장 배율 0.25에 고능력치 감쇠와 저컨디션 배율이 곱해진다")
    void scaledGrowth_appliesScaleDampingAndCondition() {
        assertThat(StatRules.scaledGrowth(1, 49, 70, 20)).isEqualTo(250);
        assertThat(StatRules.scaledGrowth(1, 50, 70, 20)).isEqualTo(188);
        assertThat(StatRules.scaledGrowth(1, 70, 70, 20)).isEqualTo(125);
        assertThat(StatRules.scaledGrowth(1, 85, 70, 20)).isEqualTo(63);
        assertThat(StatRules.scaledGrowth(1, 20, 49, 20)).isEqualTo(188);
        assertThat(StatRules.scaledGrowth(1, 20, 70, 80)).isEqualTo(125);
        assertThat(StatRules.scaledGrowth(4, 20, 70, 20)).isEqualTo(1000);
    }

    @Test
    @DisplayName("평가는 학기별 6·10·12턴(2학기 18·22·24턴)에만 있다")
    void examTurns() {
        assertThat(StatRules.examAt(6)).contains(ExamKind.MIDTERM);
        assertThat(StatRules.examAt(10)).contains(ExamKind.PERFORMANCE);
        assertThat(StatRules.examAt(12)).contains(ExamKind.FINAL);
        assertThat(StatRules.examAt(18)).contains(ExamKind.MIDTERM);
        assertThat(StatRules.examAt(22)).contains(ExamKind.PERFORMANCE);
        assertThat(StatRules.examAt(24)).contains(ExamKind.FINAL);
        IntStream.rangeClosed(1, 24).filter(turn -> turn % 12 != 6 && turn % 12 != 10 && turn % 12 != 0)
                .forEach(turn -> assertThat(StatRules.examAt(turn)).as("turn " + turn).isEmpty());
    }

    @Test
    @DisplayName("현재 성적보다 낮은 점수는 G를 내리고 높은 점수는 올리며 0~100을 벗어나지 않는다")
    void nextGrade_movesTowardScore() {
        assertThat(StatRules.nextGrade(70, 40, ExamKind.FINAL)).isEqualTo(58);
        assertThat(StatRules.nextGrade(70, 100, ExamKind.MIDTERM)).isEqualTo(79);
        assertThat(StatRules.nextGrade(70, 70, ExamKind.PERFORMANCE)).isEqualTo(70);
        assertThat(StatRules.nextGrade(100, 100, ExamKind.FINAL)).isEqualTo(100);
        assertThat(StatRules.nextGrade(0, 0, ExamKind.FINAL)).isZero();
    }

    @Test
    @DisplayName("시험 점수는 준비도·기초 능력·건강이 높을수록 오르고 스트레스가 높을수록 내려가며 0~100으로 제한된다")
    void examScore_dependsOnPreparationAbilityHealthAndStress() {
        Stats baseline = TestStats.baseline();
        int none = StatRules.examScore(ExamKind.FINAL, baseline, 0);
        int prepared = StatRules.examScore(ExamKind.FINAL, baseline, 10);

        assertThat(prepared).isGreaterThan(none);
        assertThat(StatRules.examScore(ExamKind.FINAL, baseline, 99))
                .isEqualTo(StatRules.examScore(ExamKind.FINAL, baseline, StatRules.PREPARATION_CAP));
        assertThat(StatRules.examScore(ExamKind.FINAL, stats(80, 80, 40, 10), 0)).isGreaterThan(none);
        assertThat(StatRules.examScore(ExamKind.FINAL, stats(24, 72, 18, 90), 0)).isLessThan(none);
        assertThat(StatRules.examScore(ExamKind.FINAL, stats(100, 100, 100, 0), 20)).isEqualTo(100);
        assertThat(StatRules.examScore(ExamKind.FINAL, stats(0, 0, 0, 100), 0)).isZero();
    }

    private static Stats stats(int a, int h, int c, int s) {
        Map<String, Integer> values = new HashMap<>(TestStats.baseline().toMap());
        values.put("A", a);
        values.put("P", a);
        values.put("H", h);
        values.put("C", c);
        values.put("S", s);
        return Stats.of(values);
    }
}
