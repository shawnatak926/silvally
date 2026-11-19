package com.silvally.support;

import com.silvally.entity.Player;
import com.silvally.entity.Stats;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

/** 추첨 초기값 대신 점수 계산을 손으로 검증하는 테스트가 쓰는 고정 능력치. */
public final class TestStats {

    private TestStats() {
    }

    public static Stats baseline() {
        return Stats.of(Map.of("P", 20, "F", 8, "B", 8, "I", 8, "A", 24, "G", 70,
                "C", 20, "H", 72, "S", 18, "EN", 22));
    }

    public static void pin(Player player) {
        ReflectionTestUtils.setField(player, "stats", baseline());
    }
}
