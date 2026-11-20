package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.response.FieldLearningView;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class FieldLearningServiceImpl implements FieldLearningService {
    private final NarrativeService narratives;
    private final PlayerRepository players;
    private final FieldLearningRepository learning;
    private final ActivityRecordRepository activities;
    private final CommandLogRepository history;
    private final ObjectMapper json;
    public FieldLearningServiceImpl(PlayerRepository players, FieldLearningRepository learning,
            ActivityRecordRepository activities, CommandLogRepository history, ObjectMapper json, NarrativeService narratives) {
        this.narratives = narratives;
        this.players = players; this.learning = learning; this.activities = activities; this.history = history; this.json = json;
    }
    @Override public FieldLearningView get(UUID playerId) {
        if (!players.existsById(playerId)) throw new BusinessException(ErrorCode.NOT_FOUND);
        return learning.findById(playerId).map(this::view).orElse(new FieldLearningView(null, "NOT_APPLIED", null, null, false, false, false, null, null, null, null));
    }
    @Override
    public com.silvally.dto.response.FieldLearningSummaryView summary(UUID playerId) {
        var program = get(playerId);
        var milestones = activities.findByPlayerIdOrderByIdAsc(playerId).stream()
                .filter(a -> a.getEventCode().equals("J01") || a.getEventCode().startsWith("J01_"))
                .map(a -> new com.silvally.dto.response.FieldLearningSummaryView.Milestone(a.getEventCode(),
                        a.getChoiceCode(), a.getResultCode(), json.readValue(a.getEvidenceJson(), Map.class))).toList();
        return new com.silvally.dto.response.FieldLearningSummaryView(program, milestones);
    }
    @Override @Transactional
    public FieldLearningView select(Player p, String path) {
        if (p.getPhase() != Phase.SCHOOL || p.getYear() != 3 || p.getTurn() != 3 || p.getPendingChoice() != null)
            throw new BusinessException(ErrorCode.WRONG_STEP, "현장학습은 3학년 3턴에 신청합니다.");
        if (learning.existsById(p.getId())) throw new BusinessException(ErrorCode.WRONG_STEP, "이미 현장학습에 신청했습니다.");
        FieldLearning.Path selected;
        try { selected = FieldLearning.Path.valueOf(path); }
        catch (IllegalArgumentException | NullPointerException e) { throw new BusinessException(ErrorCode.VALIDATION_ERROR); }
        return view(learning.save(new FieldLearning(p, selected)));
    }
    @Override @Transactional
    public Optional<FieldLearningView> onTurnEnd(Player p) {
        if (p.getPhase() != Phase.SCHOOL || p.getYear() != 3) return Optional.empty();
        var saved = learning.findById(p.getId());
        if (saved.isEmpty()) return Optional.empty();
        FieldLearning f = saved.get();
        String event;
        Map<String, Integer> reward = Map.of();
        if (p.getTurn() == 3 && f.getStatus() == FieldLearning.Status.SELECTED) {
            f.prepare(); event = "J01_PREPARE";
        } else if (p.getTurn() == 4 && f.getStatus() == FieldLearning.Status.PREPARING) {
            f.complete(); event = "J01_COMPLETE";
            reward = f.getPath() == FieldLearning.Path.DOMESTIC ? Map.of("P", 2, "C", 3) : Map.of("EN", 4, "C", 3);
        } else return Optional.empty();
        var before = p.getStats().values();
        StatGrowth.apply(p, reward, event);
        var actual = new LinkedHashMap<String, Integer>();
        p.getStats().values().forEach((stat, value) -> {
            if (value.intValue() != before.get(stat)) actual.put(stat, value - before.get(stat));
        });
        Map<String, Object> evidence = Map.of("fieldLearning", view(f), "statDelta", actual,
                "scene", f.getStatus() == FieldLearning.Status.COMPLETED ? "현장학습 참가를 마치고 경험을 정리했습니다." : "선발된 현장학습 참가를 준비했습니다.",
                "resultCode", f.getStatus().name());
        String text = json.writeValueAsString(evidence);
        activities.save(new ActivityRecord(p, event, f.getPath().name(), f.getStatus().name(), text));
        CommandLog scene = history.save(CommandLog.scene(p, UUID.randomUUID(), event, f.getPath().name(), f.getStatus().name(), text));
        narratives.request(scene);
        return Optional.of(view(f));
    }
    private FieldLearningView view(FieldLearning f) {
        return new FieldLearningView(f.getPath().name(), f.getStatus().name(), f.getEnglishAtApplication(), f.getFailureReason(),
                f.getStatus() != FieldLearning.Status.NOT_SELECTED, f.getPreparedTurn() != null,
                f.getCompletedTurn() != null, f.getAppliedYear(), f.getAppliedTurn(), f.getPreparedTurn(), f.getCompletedTurn());
    }
}
