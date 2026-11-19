package com.silvally.service;

import com.silvally.entity.GrowthProgress;
import com.silvally.entity.Player;
import com.silvally.entity.Stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 재학 중 능력치 증감의 공통 경로. 양수 성장은 배율을 거쳐 소수까지 누적하고 G는 건드리지 않는다. */
public final class StatGrowth {

    private StatGrowth() {
    }

    public static List<StatChange> apply(Player player, Map<String, Integer> raw, String reason) {
        Stats stats = player.getStats();
        Map<String, Integer> before = stats.values();
        int health = stats.getH();
        int stress = stats.getS();
        Map<String, Integer> delta = new LinkedHashMap<>();
        raw.forEach((stat, amount) -> {
            if (amount == 0 || "G".equals(stat)) return;
            if (amount > 0 && StatRules.TECH_STATS.contains(stat)) player.markTrained(stat);
            int applied = amount > 0 && StatRules.GROWTH_STATS.contains(stat)
                    ? accumulate(player, stat, amount, health, stress) : amount;
            delta.merge(stat, applied, Integer::sum);
        });
        stats.apply(delta);
        return StatChange.between(before, stats.values(), reason);
    }

    /** 저컨디션이면 학습 기반 A가 1점 하락한다. */
    public static List<StatChange> conditionDecline(Player player) {
        Stats stats = player.getStats();
        if (!StatRules.lowCondition(stats.getH(), stats.getS())) return List.of();
        return decline(player, "A", StatRules.LOW_CONDITION_LOSS, "LOW_CONDITION");
    }

    /** 학기 동안 한 번도 성장하지 않은 기술 능력치가 1점 하락하고, 다음 학기 기록을 새로 시작한다. */
    public static List<StatChange> neglectDecline(Player player) {
        List<StatChange> changes = new ArrayList<>();
        for (String stat : StatRules.TECH_STATS) {
            if (!player.isTrained(stat)) changes.addAll(decline(player, stat, StatRules.NEGLECT_LOSS, "NEGLECTED"));
        }
        player.resetTrainedStats();
        return changes;
    }

    private static List<StatChange> decline(Player player, String stat, int amount, String reason) {
        Stats stats = player.getStats();
        int loss = Math.min(amount, Math.max(0, stats.get(stat) - StatRules.LOSS_FLOOR));
        if (loss == 0) return List.of();
        Map<String, Integer> before = stats.values();
        stats.apply(Map.of(stat, -loss));
        return StatChange.between(before, stats.values(), reason);
    }

    private static int accumulate(Player player, String stat, int amount, int health, int stress) {
        GrowthProgress progress = player.getGrowthProgress();
        int current = player.getStats().get(stat);
        int total = progress.get(stat) + StatRules.scaledGrowth(amount, current, health, stress);
        int whole = total / GrowthProgress.UNIT;
        progress.set(stat, current + whole >= 100 ? 0 : total % GrowthProgress.UNIT);
        return whole;
    }
}
