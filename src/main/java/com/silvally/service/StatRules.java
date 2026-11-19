package com.silvally.service;

import com.silvally.entity.GrowthProgress;
import com.silvally.entity.Stats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * 능력치 초기 범위·성장 배율·손실 하한·시험 공식을 모아 둔 게임용 가상 규칙.
 * 학교 공식 산식이 아니다.
 */
public final class StatRules {

    public static final String RULE_SOURCE = "GAME_RULE";

    public record Range(int min, int max) {
    }

    public static final Map<String, Range> INITIAL_RANGES = initialRanges();

    /** 배율을 적용하는 기술(P·F·B·I)·학습(A)·영어(EN). 손실과 C·H·S는 그대로 반영한다. */
    public static final Set<String> GROWTH_STATS = Set.of("P", "F", "B", "I", "A", "EN");
    public static final double BASE_GROWTH_SCALE = 0.25;

    public static final List<String> TECH_STATS = List.of("P", "F", "B", "I");
    /** 하락으로는 이 값 아래로 내려가지 않는다. 한 번의 하락은 1점이다. */
    public static final int LOSS_FLOOR = 5;
    public static final int LOW_CONDITION_LOSS = 1;
    public static final int NEGLECT_LOSS = 1;

    private StatRules() {
    }

    private static Map<String, Range> initialRanges() {
        Map<String, Range> ranges = new LinkedHashMap<>();
        ranges.put("P", new Range(19, 25));
        ranges.put("F", new Range(5, 11));
        ranges.put("B", new Range(5, 11));
        ranges.put("I", new Range(5, 11));
        ranges.put("A", new Range(22, 28));
        ranges.put("G", new Range(60, 80));
        ranges.put("C", new Range(19, 25));
        ranges.put("H", new Range(64, 80));
        ranges.put("S", new Range(12, 24));
        ranges.put("EN", new Range(18, 26));
        return Map.copyOf(ranges);
    }

    public static Stats rollInitial(long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        Map<String, Integer> rolled = new LinkedHashMap<>();
        for (String stat : Stats.KEYS) {
            Range range = INITIAL_RANGES.get(stat);
            rolled.put(stat, random.nextInt(range.min(), range.max() + 1));
        }
        return Stats.of(rolled);
    }

    public static long statSeed(String seed) {
        try {
            UUID uuid = UUID.fromString(seed);
            return uuid.getMostSignificantBits() ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 17);
        } catch (IllegalArgumentException e) {
            return seed.hashCode();
        }
    }

    public static double highStatDamping(int current) {
        if (current >= 85) return 0.25;
        if (current >= 70) return 0.5;
        if (current >= 50) return 0.75;
        return 1.0;
    }

    public static double conditionFactor(int health, int stress) {
        if (health < 30 || stress >= 80) return 0.5;
        if (health < 50 || stress >= 60) return 0.75;
        return 1.0;
    }

    public static boolean lowCondition(int health, int stress) {
        return health < 30 || stress >= 80;
    }

    /** 원래 성장량을 배율·감쇠·컨디션으로 줄인 1/1000점 단위 성장분. */
    public static int scaledGrowth(int raw, int current, int health, int stress) {
        return (int) Math.round(raw * GrowthProgress.UNIT * BASE_GROWTH_SCALE
                * highStatDamping(current) * conditionFactor(health, stress));
    }
}
