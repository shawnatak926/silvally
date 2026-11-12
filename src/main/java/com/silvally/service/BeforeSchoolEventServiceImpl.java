package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.EventView;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** EventFlowService의 회차 잠금·트랜잭션 안에서 비포스쿨 규칙만 적용합니다. */
@Service
public class BeforeSchoolEventServiceImpl implements BeforeSchoolEventService {
    private final EventDefRepository events;
    private final EventChoiceRepository choices;
    private final ActivityRecordRepository records;
    private final CommandLogRepository history;
    private final JsonMapper json = JsonMapper.builder().build();
    public BeforeSchoolEventServiceImpl(EventDefRepository events, EventChoiceRepository choices,
                                    ActivityRecordRepository records, CommandLogRepository history) {
        this.events = events; this.choices = choices; this.records = records; this.history = history;
    }
    public String currentEvent(Player p) { return "BS%02d".formatted(p.getBeforeSchoolDay()); }
    private Set<String> visited(Player p, String event) {
        var visited = new HashSet<String>();
        records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), event).forEach(r -> visited.add(r.getChoiceCode()));
        // 개편 전 단일 방문 command_log를 그대로 인정하며 다시 보상을 주지 않습니다.
        history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), event).forEach(r -> visited.add(r.getChoiceCode()));
        visited.remove("FINISH_EXPLORATION");
        visited.remove(null);
        return visited;
    }
    public EventView view(Player p, String event) {
        EventDef def = events.findById(new EventDef.Id(p.getRulesVersion(), event))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        var visited = visited(p, event);
        boolean current = p.getPhase() == Phase.BEFORE_SCHOOL && event.equals(currentEvent(p));
        return new EventView(event, def.getTitle(), def.getSituation(), def.isRequired(),
            choices.findByEventCodes(p.getRulesVersion(), List.of(event)).stream().map(c -> {
                boolean enabled = current && !visited.contains(c.getId().choiceCode());
                return new EventView.Choice(c.getId().choiceCode(), c.getLabel(), c.getApCost(), enabled,
                    enabled ? null : "이미 선택했거나 현재 진행할 수 없는 활동입니다.");
            }).toList());
    }
    public MutationResult apply(Player p, String event, ChooseEventRequest request) {
        if (p.getPhase() != Phase.BEFORE_SCHOOL) throw new BusinessException(ErrorCode.WRONG_PHASE);
        if (!event.equals(currentEvent(p))) throw new BusinessException(ErrorCode.WRONG_STEP);
        if (!request.isSelectionValid()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        Set<String> visited = visited(p, event);
        int required = p.getBeforeSchoolDay() <= 2 ? 2 : 1;
        int remaining = required - visited.size();
        List<String> selected;
        if (required == 2) {
            // 구 회차의 1일차 단일 선택도 나머지 한 곳을 제출할 수 있습니다.
            if (request.choiceCodes() == null && !(event.equals("BS01") && visited.size() == 1))
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            selected = request.choiceCodes() == null ? List.of(request.choiceCode()) : request.choiceCodes();
        } else {
            if (request.choiceCodes() != null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            selected = List.of(request.choiceCode());
        }
        if (selected.size() != remaining || new HashSet<>(selected).size() != selected.size()
                || selected.stream().anyMatch(visited::contains))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        // 전부 검증한 뒤에만 상태와 이력을 변경합니다.
        var activities = selected.stream().map(code -> choices.findById(
                new EventChoice.Id(p.getRulesVersion(), event, code))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR))).toList();
        Map<String, Integer> before = p.getStats().values();
        var completed = new ArrayList<Map<String, Object>>();
        for (EventChoice choice : activities) {
            var effect = json.readTree(choice.getEffectJson());
            Map<String, Integer> old = p.getStats().values();
            var delta = new LinkedHashMap<String, Integer>();
            effect.path("stats").properties().forEach(e -> delta.put(e.getKey(), e.getValue().asInt()));
            p.getStats().apply(delta);
            Map<String, Integer> actual = difference(old, p.getStats().values());
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("choiceCode", choice.getId().choiceCode());
            evidence.put("statDelta", actual);
            evidence.put("tags", effect.path("tags"));
            evidence.put("scene", choice.getLabel() + " 활동을 마쳤습니다.");
            evidence.put("resultCode", "COMPLETED");
            String saved = json.writeValueAsString(evidence);
            records.save(new ActivityRecord(p, event, choice.getId().choiceCode(), "COMPLETED", saved));
            history.save(CommandLog.activity(p, event, choice.getId().choiceCode(), saved));
            completed.add(evidence);
        }
        p.nextBeforeSchoolDay();
        return new MutationResult(p.view(), event, 0, difference(before, p.getStats().values()),
                "COMPLETED", Map.of("activities", completed,
                "nextStep", p.getPhase() == Phase.CAREER_PICK ? "CAREER_PREFERENCE" : currentEvent(p)));
    }
    private Map<String, Integer> difference(Map<String, Integer> before, Map<String, Integer> after) {
        var result = new LinkedHashMap<String, Integer>();
        after.forEach((stat, value) -> { if (value.intValue() != before.get(stat)) result.put(stat, value - before.get(stat)); });
        return result;
    }
}
