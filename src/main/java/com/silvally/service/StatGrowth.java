package com.silvally.service;

import com.silvally.entity.GrowthProgress;
import com.silvally.entity.Player;
import com.silvally.entity.Stats;

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
            int applied = amount > 0 && StatRules.GROWTH_STATS.contains(stat)
                    ? accumulate(player, stat, amount, health, stress) : amount;
            delta.merge(stat, applied, Integer::sum);
        });
        stats.apply(delta);
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
