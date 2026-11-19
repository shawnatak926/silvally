package com.silvally.service;

import com.silvally.entity.ExamKind;
import com.silvally.entity.GrowthProgress;
import com.silvally.entity.Stats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public static final int PREPARATION_PER_SPARE_AP = 2;
    public static final int ACADEMIC_FOCUS_PREPARATION = 2;
    public static final int PREPARATION_CAP = 20;
    /** 시험 한 번에 더해지는 스트레스. 다른 사건이 시험 부담을 다룰 때도 이 값을 쓰고 중복 적용하지 않는다. */
    public static final int EXAM_STRESS = 2;
    private static final Map<Integer, ExamKind> EXAM_TURNS = Map.of(
            6, ExamKind.MIDTERM, 10, ExamKind.PERFORMANCE, 12, ExamKind.FINAL);
    private static final Map<ExamKind, Double> EXAM_WEIGHTS = Map.of(
            ExamKind.MIDTERM, 0.3, ExamKind.PERFORMANCE, 0.2, ExamKind.FINAL, 0.4);

    public static int semesterOf(int turn) {
        return turn <= 12 ? 1 : 2;
    }

    /** 1학기 6·10·12턴, 2학기 18·22·24턴. */
    public static Optional<ExamKind> examAt(int turn) {
        return Optional.ofNullable(EXAM_TURNS.get(semesterOf(turn) == 1 ? turn : turn - 12));
    }

    /** 남긴 AP는 시험 공부 시간으로, 학업 집중 학기는 턴마다 추가 준비도로 본다. */
    public static int preparationGain(int spareAp, boolean academicFocus) {
        return Math.max(0, spareAp) * PREPARATION_PER_SPARE_AP + (academicFocus ? ACADEMIC_FOCUS_PREPARATION : 0);
    }

    /** 준비도·기초 능력(A·P, 수행평가는 C 포함)·건강·스트레스로 정하는 0~100점. */
    public static int examScore(ExamKind kind, Stats stats, int preparation) {
        double base = kind == ExamKind.PERFORMANCE
                ? 0.5 * stats.getA() + 0.3 * stats.getP() + 0.2 * stats.getC()
                : 0.7 * stats.getA() + 0.3 * stats.getP();
        double score = 25 + base + 1.5 * Math.min(preparation, PREPARATION_CAP)
                + (stats.getH() - 60) / 4.0 - Math.max(0, stats.getS() - 30) / 2.0;
        return (int) Math.max(0, Math.min(100, Math.round(score)));
    }

    /** 현재 성적을 평가 점수 쪽으로 가중치만큼 옮긴다. 점수가 낮으면 성적도 내려간다. */
    public static int nextGrade(int grade, int score, ExamKind kind) {
        long next = Math.round(grade + EXAM_WEIGHTS.get(kind) * (score - grade));
        return (int) Math.max(0, Math.min(100, next));
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
