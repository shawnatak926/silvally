package com.silvally.service;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** 장면과 관련된 학교 자료만 골라 서사 맥락 스냅샷에 넣는다. */
@Component
public class SchoolEvidenceSelector {

    static final int MAX_ITEMS = 4;
    static final int MAX_FACT_LENGTH = 400;

    private static final Map<String, List<String>> SCENE_SOURCES = scenes();
    private static final Map<String, List<String>> COMPANIONS = Map.of(
            "GLOBAL_SELECTION", List.of("GLOBAL_PERIOD_NOTE"),
            "GLOBAL_PREPARATION", List.of("GLOBAL_PERIOD_NOTE"),
            "INFRASTRUCTURE", List.of("REBUILD_PLAN"),
            "DORM", List.of("REBUILD_PLAN"));

    private final SchoolLifeContent content;
    private final ObjectMapper json;

    public SchoolEvidenceSelector(SchoolLifeContent content, ObjectMapper json) {
        this.content = content;
        this.json = json;
    }

    public List<NarrativeContext.SchoolEvidence> select(String sceneId, String choiceCode, String evidenceJson) {
        Set<String> codes = new LinkedHashSet<>();
        recordedSource(evidenceJson).ifPresent(codes::add);
        codes.addAll(sceneSources(sceneId, choiceCode));
        Set<String> withCompanions = new LinkedHashSet<>();
        for (String code : codes) {
            withCompanions.add(code);
            withCompanions.addAll(COMPANIONS.getOrDefault(code, List.of()));
        }
        return withCompanions.stream()
                .map(content::source)
                .filter(Objects::nonNull)
                .limit(MAX_ITEMS)
                .map(SchoolEvidenceSelector::evidence)
                .toList();
    }

    private Optional<String> recordedSource(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode code = json.readTree(evidenceJson).path("source").path("code");
            return code.isString() ? Optional.of(code.asString()) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static List<String> sceneSources(String sceneId, String choiceCode) {
        if (sceneId == null) {
            return List.of();
        }
        if (sceneId.equals("J01")) {
            return "SILICON_VALLEY".equals(choiceCode) ? List.of("GLOBAL_SELECTION") : List.of("CAREER_SUPPORT");
        }
        return SCENE_SOURCES.entrySet().stream()
                .filter(entry -> sceneId.startsWith(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(List.of());
    }

    private static NarrativeContext.SchoolEvidence evidence(Map<String, Object> source) {
        String fact = String.valueOf(source.get("fact"));
        return new NarrativeContext.SchoolEvidence(
                (String) source.get("code"),
                (String) source.get("title"),
                (String) source.get("url"),
                source.get("year") instanceof Number year ? year.intValue() : null,
                (String) source.get("appliesTo"),
                (String) source.get("classification"),
                fact.length() <= MAX_FACT_LENGTH ? fact : fact.substring(0, MAX_FACT_LENGTH),
                (String) source.get("notice"));
    }

    private static Map<String, List<String>> scenes() {
        Map<String, List<String>> scenes = new LinkedHashMap<>();
        scenes.put("BS01", List.of("INFRASTRUCTURE", "DORM"));
        scenes.put("BS02", List.of("CURRICULUM", "CAREER_PATHS"));
        scenes.put("BS03", List.of("CURRICULUM"));
        scenes.put("BS04", List.of("NARSHA"));
        scenes.put("BS05", List.of("CAREER_PATHS", "QUALIFICATION_SUPPORT"));
        scenes.put("FOCUS_", List.of("CURRICULUM"));
        scenes.put("VACATION_", List.of("VACATION_CAMP"));
        scenes.put("NARSHA_", List.of("NARSHA"));
        scenes.put("SOFTWAREWAVE", List.of("NARSHA", "EXPO"));
        scenes.put("STRESS_SUPPORT", List.of("NEUROW", "DORM"));
        scenes.put("SCHOOL_DOUBTS", List.of("NEUROW", "DORM"));
        scenes.put("WITHDRAWAL_DECISION", List.of("NEUROW", "CAREER_PATHS"));
        return scenes;
    }
}
