package com.silvally.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record StatChange(String stat, int delta, String reason) {

    static List<StatChange> between(Map<String, Integer> before, Map<String, Integer> after, String reason) {
        List<StatChange> changes = new ArrayList<>();
        after.forEach((stat, value) -> {
            int delta = value - before.get(stat);
            if (delta != 0) changes.add(new StatChange(stat, delta, reason));
        });
        return changes;
    }
}
