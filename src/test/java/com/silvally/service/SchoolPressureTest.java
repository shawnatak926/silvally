package com.silvally.service;

import com.silvally.entity.Player;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SchoolPressureTest {
    private final SchoolLifeServiceImpl life = new SchoolLifeServiceImpl(null, null, null, null, null);
    Player at(String seed, int year, int turn, int health, int stress) {
        Player p = Player.admission("압박 검증", "OPEN"); p.startSchool(3);
        ReflectionTestUtils.setField(p, "seed", seed);
        ReflectionTestUtils.setField(p, "year", year); ReflectionTestUtils.setField(p, "turn", turn);
        p.getStats().apply(Map.of("H", health - p.getStats().getH(), "S", stress - p.getStats().getS())); return p;
    }
    @Test void everyPressureRangeCapRecoveryAndClampingAreMeasuredAsApplied() {
        Set<String> observed = new HashSet<>();
        for (int seed = 0; seed < 128; seed++) for (int turn = 1; turn <= 24; turn++) {
            Player p = at("pressure-" + seed, 1, turn, 80, 20);
            if (turn >= 8 && turn <= 23) p.setNarshaStage("STARTED");
            var load = life.applyTurnPressure(p);
            @SuppressWarnings("unchecked") var sources = (List<Map<String, Object>>) load.get("sources");
            int total = sources.stream().mapToInt(s -> (Integer) s.get("delta")).sum();
            int positives = sources.stream().mapToInt(s -> Math.max(0, (Integer) s.get("delta"))).sum();
            assertThat(positives).isLessThanOrEqualTo(5);
            assertThat(total).isEqualTo((Integer) load.get("total")).isEqualTo(p.getStats().getS() - 20);
            boolean recovery = sources.stream().anyMatch(s -> s.get("code").equals("DAILY_RECOVERY"));
            int t = (turn - 1) % 12 + 1;
            assertThat(recovery).isEqualTo(!Set.of(6, 10, 12).contains(t) && !(turn >= 20 && turn <= 23));
            for (var source : sources) {
                String code = (String) source.get("code"); int delta = (Integer) source.get("delta"); observed.add(code);
                switch (code) {
                    case "SCHOOL_ROUTINE", "NARSHA_WORK", "ASSESSMENT_PREP" -> assertThat(delta).isBetween(0, 1);
                    case "MIDTERM_PREP", "FINAL_PREP", "NARSHA_DEADLINE" -> assertThat(delta).isBetween(1, 3);
                    case "MIDTERM", "ASSESSMENT", "FINAL" -> assertThat(delta).isBetween(1, 5);
                    case "DAILY_RECOVERY" -> assertThat(delta).isEqualTo(-2);
                    default -> fail("unknown pressure source " + code);
                }
            }
        }
        assertThat(observed).contains("MIDTERM", "FINAL", "ASSESSMENT", "NARSHA_DEADLINE", "NARSHA_WORK", "DAILY_RECOVERY");
        for (int stress : List.of(0, 99, 100)) {
            var p = at("clamp", 1, 1, 80, stress); var load = life.applyTurnPressure(p);
            assertThat(p.getStats().getS()).isBetween(0, 100);
            assertThat(load.get("total")).isEqualTo(p.getStats().getS() - stress);
        }
    }
    @Test void health39DoesNotRecoverBut40DoesAndSameSeedProducesSamePressure() {
        var low = at("same", 1, 1, 39, 20); var healthy = at("same", 1, 1, 40, 20);
        life.applyTurnPressure(low); life.applyTurnPressure(healthy);
        assertThat(low.getStats().getS() - healthy.getStats().getS()).isEqualTo(2);
        var other = at("same", 1, 1, 40, 20); life.applyTurnPressure(other);
        assertThat(other.getStats().getS()).isEqualTo(healthy.getStats().getS());
    }
    @Test void simulated72TurnSupportFrequencyStaysBoundedWithoutMandatoryDailyRest() {
        int totalSupports = 0, maxSupports = 0;
        for (int seed = 0; seed < 128; seed++) {
            var p = at("run-" + seed, 1, 1, 80, 18); int count = 0;
            for (int year = 1; year <= 3; year++) for (int turn = 1; turn <= 24; turn++) {
                ReflectionTestUtils.setField(p, "year", year); ReflectionTestUtils.setField(p, "turn", turn);
                p.setNarshaStage(year == 1 && turn >= 8 && turn <= 23 ? "STARTED" : year == 2 && turn >= 2 && turn <= 22 ? "RESUMED" : null);
                if (p.getStats().getS() >= 60) { count++; p.getStats().apply(Map.of("S", -12)); }
                life.applyTurnPressure(p);
            }
            totalSupports += count; maxSupports = Math.max(maxSupports, count);
        }
        double mean = totalSupports / 128.0;
        System.out.println("128 seeded 72-turn models; mean support count=" + mean + ", max=" + maxSupports);
        assertThat(mean).isLessThan(8); assertThat(maxSupports).isLessThan(12);
    }
}
