package com.silvally.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;

/** Versioned school evidence and fictional schedules, shared with UI and narrative evidence. */
@Component
public class SchoolLifeContent {
    public record Choice(String code, String label, Map<String, Integer> delta, String tag, int cost, String description) { }
    public record Event(String code, String title, String situation, boolean required, List<Choice> choices, String source) { }
    public record Activity(List<Integer> years, int minTurn, int maxTurn, Event event) { }
    public record Scene(int year, int turn, String title, String premise, String source) { }
    public record Content(Map<String, Map<String, Object>> schoolSources, List<Scene> schoolScenes, List<Activity> schoolActivities) { }
    private final Content content;
    public SchoolLifeContent(ObjectMapper json) throws IOException {
        try (var in = new ClassPathResource("rules/school-content.json").getInputStream()) {
            content = json.readValue(in.readAllBytes(), Content.class);
        }
    }
    public List<Activity> activities() { return content.schoolActivities(); }
    public List<Scene> scenes() { return content.schoolScenes(); }
    public Map<String, Object> source(String code) { return content.schoolSources().get(code); }
    public Collection<Map<String, Object>> sources() { return content.schoolSources().values(); }
    public List<Event> candidates(int year, int turn) {
        return activities().stream().filter(a -> a.years().contains(year) && a.minTurn() <= turn && turn <= a.maxTurn())
                .map(Activity::event).toList();
    }
}
