package com.silvally.service;

import com.silvally.dto.response.FieldLearningView;
import com.silvally.entity.Player;
import java.util.*;

public interface FieldLearningService {
    com.silvally.dto.response.FieldLearningSummaryView summary(UUID playerId);
    FieldLearningView get(UUID playerId);
    FieldLearningView select(Player player, String path);
    Optional<FieldLearningView> onTurnEnd(Player player);
}
