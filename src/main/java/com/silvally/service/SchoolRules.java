package com.silvally.service;

import com.silvally.entity.Player;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SchoolRules {
    public static final int TURNS_PER_YEAR = 10;
    public static final int AP_PER_TURN = 3;
    public static final int LAST_YEAR = 3;
    static final List<String> BASE_FOCUS = List.of("P", "A");
    static final int REST_H_PER_AP = 2;
    static final int FATIGUE_H = 2;

    public List<String> focusStats(Player player) {
        return BASE_FOCUS;
    }

    public Map<String, Integer> turnGrowth(Player player) {
        Map<String, Integer> delta = new LinkedHashMap<>();
        focusStats(player).forEach(stat -> delta.merge(stat, 1, Integer::sum));
        int restH = player.getAp() > 0 ? player.getAp() * REST_H_PER_AP : -FATIGUE_H;
        delta.merge("H", restH, Integer::sum);
        return delta;
    }

    public boolean isLastTurnOfYear(Player player) {
        return player.getTurn() >= TURNS_PER_YEAR;
    }
}
