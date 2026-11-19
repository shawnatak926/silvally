package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.response.*;
import com.silvally.entity.Player;
import com.silvally.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdmissionResultServiceImpl implements AdmissionResultService {
    private final PlayerRepository players;
    private final ActivityRecordRepository records;
    private final ObjectMapper json;
    public AdmissionResultServiceImpl(PlayerRepository players, ActivityRecordRepository records, ObjectMapper json) {
        this.players = players; this.records = records; this.json = json;
    }
    @Override
    public AdmissionResultView get(UUID playerId, String eventCode) {
        Player p = players.findById(playerId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!Set.of("AD02", "AD05").contains(eventCode)) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        var saved = records.findByPlayerIdAndEventCodeOrderByIdAsc(playerId, eventCode);
        if (saved.isEmpty()) throw new BusinessException(ErrorCode.WRONG_STEP, "아직 해당 전형의 제출 결과가 없습니다.");
        var last = saved.getLast();
        JsonNode root = json.readTree(last.getEvidenceJson());
        if (root.has("admissionResult")) return json.readValue(root.path("admissionResult").toString(), AdmissionResultView.class);
        // 구 회차를 현재 점수로 다시 채점하지 않고 당시 저장한 판정만 읽습니다.
        JsonNode d = root.path("decision");
        var decision = new AdmissionResultView.Decision(d.path("passed").asBoolean(), d.path("shieldActivated").asBoolean(),
                d.path("rank").asInt(), d.path("limit").asInt(), d.path("score").asDouble(),
                d.path("generalScore").asDouble(), d.path("generalRank").asInt());
        SchoolInterviewEvaluationView interview = null;
        if (root.has("interview")) {
            JsonNode old = root.path("interview");
            Map<String, Integer> components = new LinkedHashMap<>();
            old.path("components").properties().forEach(e -> components.put(e.getKey(), e.getValue().asInt()));
            interview = new SchoolInterviewEvaluationView(old.path("score").asInt(), components,
                    old.path("source").asString("RULE_BASED"), "legacy-unversioned", Map.of());
        }
        return new AdmissionResultView(eventCode, last.getResultCode(), p.getAdmissionTrack(),
                decision.shieldActivated() ? "GENERAL" : p.getAdmissionTrack(), decision, Map.of(), interview,
                List.of("LEGACY_RESULT_WITHOUT_RUBRIC_EVIDENCE"), decision.passed()
                ? eventCode.equals("AD02") ? "ADMISSION_2" : "BEFORE_SCHOOL" : "ENDED",
                decision.passed() && eventCode.equals("AD05") ? 1 : null);
    }
}
