package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.*;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final NarrativeService narratives;
    private final FieldLearningService fieldLearning;
    private final SchoolCalendar calendar;
    private final PlayerRepository players;
    private final EventDefRepository events;
    private final EventChoiceRepository choices;
    private final ActivityRecordRepository activities;
    private final CommandLogRepository history;
    private final CommandExecutor commands;
    private final SchoolRules rules;
    private final ObjectMapper json;
    public EventServiceImpl(PlayerRepository players, EventDefRepository events, EventChoiceRepository choices,
            ActivityRecordRepository activities, CommandLogRepository history, CommandExecutor commands,
            SchoolRules rules, ObjectMapper json, SchoolCalendar calendar, FieldLearningService fieldLearning, NarrativeService narratives) {
        this.narratives = narratives;
        this.fieldLearning = fieldLearning;
        this.calendar = calendar;
        this.players = players; this.events = events; this.choices = choices; this.activities = activities;
        this.history = history; this.commands = commands; this.rules = rules; this.json = json;
    }
    private Player player(UUID id) {
        return players.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    private EventDef event(Player p, String code) {
        return events.findById(new EventDef.Id(p.getRulesVersion(), code))
                .filter(e -> e.getPhase() == Phase.SCHOOL).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    private boolean completed(Player p, String code) {
        return !activities.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), code).isEmpty();
    }
    private boolean available(Player p, EventDef e) {
        return p.getPhase() == Phase.SCHOOL && p.getPendingChoice() == null && p.getYear() == e.getYear()
                && p.getTurn() >= e.getMinTurn() && p.getTurn() <= e.getMaxTurn() && !completed(p, e.getId().eventCode()) && calendar.available(p, e.getId().eventCode());
    }
    @Override public List<EventView> list(UUID id) {
        Player p = player(id);
        if (p.getPhase() != Phase.SCHOOL || p.getPendingChoice() != null) return List.of();
        return events.findEvents(p.getRulesVersion(), Phase.SCHOOL, p.getYear()).stream()
                .filter(e -> available(p, e)).map(e -> view(p, e)).toList();
    }
    @Override public EventView detail(UUID id, String code) { Player p = player(id); return view(p, event(p, code)); }
    private EventView view(Player p, EventDef e) {
        boolean current = available(p, e);
        return new EventView(e.getId().eventCode(), e.getTitle(), e.getSituation(), e.isRequired(),
                choices.findByEventCodes(p.getRulesVersion(), List.of(e.getId().eventCode())).stream().map(c -> {
                    boolean enabled = current && p.getAp() >= c.getApCost();
                    return new EventView.Choice(c.getId().choiceCode(), c.getLabel(), c.getApCost(), enabled,
                            enabled ? null : "이미 선택했거나 시기·AP·필수 선택 조건을 만족하지 않습니다.");
                }).toList());
    }
    @Override public void requireNoPendingEvent(Player p) {
        List<String> pending = events.findEvents(p.getRulesVersion(), Phase.SCHOOL, p.getYear()).stream()
                .filter(EventDef::isRequired).filter(e -> available(p, e)).map(e -> e.getId().eventCode()).toList();
        if (!pending.isEmpty()) throw new BusinessException(ErrorCode.CHOICE_REQUIRED, Map.of("events", pending));
    }
    @Override @Transactional
    public MutationResult choose(UUID id, String code, long version, String key, ChooseEventRequest request) {
        if (request == null || !request.isSelectionValid() || request.choiceCodes() != null)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        var delta = new LinkedHashMap<String, Integer>();
        var details = new LinkedHashMap<String, Object>();
        int[] cost = {0};
        MutationResult response = commands.execute(id, version, key,
                new CommandSpec<>("EVENT_CHOICE", code, request, MutationResult.class), p -> {
                    rules.requireSchool(p); rules.requireNoPendingChoice(p);
                    EventDef e = event(p, code);
                    if (!available(p, e)) throw new BusinessException(ErrorCode.WRONG_STEP);
                    EventChoice c = choices.findById(new EventChoice.Id(p.getRulesVersion(), code, request.choiceCode()))
                            .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
                    if (p.getAp() < c.getApCost()) throw new BusinessException(ErrorCode.INSUFFICIENT_AP);
                    var effect = json.readTree(c.getEffectJson());
                    Map<String, Integer> before = p.getStats().values();
                    Map<String, Integer> growth = new LinkedHashMap<>();
                    effect.path("stats").properties().forEach(entry -> growth.put(entry.getKey(), entry.getValue().asInt()));
                    StatGrowth.apply(p, growth, code); p.spendAp(c.getApCost()); cost[0] = c.getApCost();
                    p.getStats().values().forEach((stat, value) -> {
                        if (value.intValue() != before.get(stat)) delta.put(stat, value - before.get(stat));
                    });
                    details.put("choiceCode", c.getId().choiceCode()); details.put("statDelta", delta);
                    details.put("sceneSource", "MOCK"); details.put("sceneStatus", "READY");
                    details.put("tags", effect.path("tags")); details.put("scene", effect.path("scene").asString(c.getLabel()));
                    String result = calendar.apply(p, code, c.getId().choiceCode(), details);
                    if (code.equals("J01")) {
                        var selection = fieldLearning.select(p, c.getId().choiceCode());
                        result = selection.status();
                        details.put("fieldLearning", json.convertValue(selection, Map.class));
                        details.put("scene", selection.selected() ? "현장학습에 신청하여 선발되었습니다."
                                : "해외 현장학습 영어 기준에 미달하여 선발되지 않았습니다.");
                    }
                    details.put("resultCode", result);
                    var activity = activities.save(new ActivityRecord(p, code, c.getId().choiceCode(), result, json.writeValueAsString(details)));
                    details.put("sceneId", activity.getId());
                }, p -> new MutationResult(p.view(), code, cost[0], delta, String.valueOf(details.get("resultCode")), details));
        // 공통 명령 로그 한 건에 장면·선택을 붙여 인생 기록과 멱등 응답을 함께 보존합니다.
        CommandLog log = history.findByPlayerIdAndCommandId(id, CommandExecutor.parseIdempotencyKey(key)).orElseThrow();
        log.recordActivity(request.choiceCode(), json.writeValueAsString(response.details()));
        narratives.request(log);
        return response;
    }
}
