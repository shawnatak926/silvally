package com.silvally.service;

import com.silvally.dto.response.EventView;
import com.silvally.entity.Player;
import java.util.*;

public interface SchoolLifeService {
    Optional<EventView> current(Player player);
    String choose(Player player, String eventCode, String choiceCode, Map<String, Object> evidence);
    Map<String, Object> applyTurnPressure(Player player);
    void recordTurnScene(Player player, Map<String, Object> stressLoad);
    static boolean owns(String code) {
        return code.startsWith("DAILY_") || Set.of("STRESS_SUPPORT", "SCHOOL_DOUBTS", "WITHDRAWAL_DECISION").contains(code);
    }
}
