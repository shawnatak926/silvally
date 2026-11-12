package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.EventView;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class EventFlowServiceImpl implements EventFlowService {
    private final PlayerRepository players;
    private final EventDefRepository events;
    private final EventChoiceRepository choices;
    private final ActivityRecordRepository records;
    private final EventCommandRepository commands;
    private final BeforeSchoolEventService beforeSchool;
    private final AdmissionPolicy policy;
    private final JsonMapper json = JsonMapper.builder().build();

    public EventFlowServiceImpl(PlayerRepository players, EventDefRepository events,
            EventChoiceRepository choices, ActivityRecordRepository records,
            EventCommandRepository commands, AdmissionPolicy policy, BeforeSchoolEventService beforeSchool) {
        this.players = players; this.events = events; this.choices = choices;
        this.records = records; this.commands = commands; this.policy = policy; this.beforeSchool = beforeSchool;
    }
    private Player player(UUID id) {
        return players.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    public List<EventView> list(UUID id) {
        Player p = player(id);
        if (p.getPhase() == Phase.BEFORE_SCHOOL) return List.of(beforeSchool.view(p, beforeSchool.currentEvent(p)));
        if (p.getPhase() != Phase.ADMISSION_1 && p.getPhase() != Phase.ADMISSION_2) return List.of();
        return List.of(view(p, p.getAdmissionEvent()));
    }
    public EventView detail(UUID id, String event) { return view(player(id), event); }
    private EventView view(Player p, String code) {
        if (code.startsWith("BS")) return beforeSchool.view(p, code);
        EventDef def = events.findById(new EventDef.Id(p.getRulesVersion(), code))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        boolean current = (p.getPhase() == Phase.ADMISSION_1 || p.getPhase() == Phase.ADMISSION_2)
                && code.equals(p.getAdmissionEvent());
        var options = choices.findByEventCodes(p.getRulesVersion(), List.of(code)).stream().map(c -> {
            boolean enabled = current && (!code.equals("AD01") || policy.isEligible(p, c.getId().choiceCode()));
            return new EventView.Choice(c.getId().choiceCode(), c.getLabel(), c.getApCost(), enabled,
                    enabled ? null : "현재 단계 또는 지원 자격에 맞지 않습니다.");
        }).toList();
        return new EventView(code, def.getTitle(), def.getSituation(), def.isRequired(), options);
    }
    @Transactional
    public MutationResult choose(UUID id, String event, long version, String key, ChooseEventRequest request) {
        if (key == null || key.isBlank() || key.length() > 200 || request == null)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        Player p = players.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String fingerprint = json.writeValueAsString(List.of(event, request));
        var previous = commands.findByPlayerIdAndRequestKey(id, key);
        if (previous.isPresent()) {
            if (!previous.get().getRequestJson().equals(fingerprint))
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            return json.readValue(previous.get().getResponseJson(), MutationResult.class);
        }
        if (p.getVersion() != version) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        if (event.startsWith("BS")) {
            var result = beforeSchool.apply(p, event, request);
            p.markCommand(UUID.randomUUID());
            players.flush();
            var response = new MutationResult(p.view(), result.actionCode(), result.spentAp(),
                    result.statDelta(), result.resultCode(), result.details());
            commands.save(new EventCommand(id, key, fingerprint, json.writeValueAsString(response)));
            return response;
        }
        if (!request.isSelectionValid() || request.choiceCodes() != null)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (p.getPhase() != Phase.ADMISSION_1 && p.getPhase() != Phase.ADMISSION_2)
            throw new BusinessException(ErrorCode.WRONG_PHASE);
        if (!event.equals(p.getAdmissionEvent())) throw new BusinessException(ErrorCode.WRONG_STEP);
        String code = request.choiceCode();
        if (code == null || code.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        EventChoice choice = choices.findById(new EventChoice.Id(p.getRulesVersion(), event, code))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
        JsonNode effect = json.readTree(choice.getEffectJson());
        Map<String, Object> details = new LinkedHashMap<>();
        String result = "SELECTED";
        switch (event) {
            case "AD01" -> {
                if (!policy.isEligible(p, code)) throw new BusinessException(ErrorCode.LOCKED);
                p.selectAdmissionTrack(code);
            }
            case "AD02", "AD05" -> {
                boolean first = event.equals("AD02");
                double essay = effect.path("essay").asDouble();
                double grades = effect.path("grades").asDouble();
                double record = effect.path("record").asDouble();
                double score = first ? policy.documentScore(p.getAdmissionTrack(), essay, grades, record)
                        : p.finalScore(effect.path("score").asDouble());
                double general = first ? policy.documentScore("GENERAL", essay, grades, record) : score;
                var decision = policy.decide(p.getSeed(), first ? 1 : 2, p.getAdmissionTrack(), score, general);
                details.put("decision", decision);
                details.put("submitted", effect);
                result = decision.passed() ? "PASSED" : "FAILED";
                if (decision.shieldActivated()) result = "SHIELD_" + result;
                if (decision.passed() && first) p.passDocuments();
                else p.finishAdmission(decision.passed());
            }
            case "AD03", "AD04" -> p.prepare(event, effect.path("score").asDouble());
            default -> throw new BusinessException(ErrorCode.WRONG_STEP);
        }
        records.save(new ActivityRecord(p, event, code, result, json.writeValueAsString(details)));
        p.markCommand(UUID.randomUUID());
        players.flush();
        var response = new MutationResult(p.view(), event, 0, Map.of(), result, details);
        commands.save(new EventCommand(id, key, fingerprint, json.writeValueAsString(response)));
        return response;
    }
}
