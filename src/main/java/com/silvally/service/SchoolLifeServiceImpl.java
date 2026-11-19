package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.response.*;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static com.silvally.service.SchoolLifeContent.*;

/** Fictional balancing rules; school documents do not define these probabilities or stat effects. */
@Service
@Transactional
public class SchoolLifeServiceImpl implements SchoolLifeService {
    private final SchoolEventDrawRepository draws;
    private final SchoolLifeContent content;
    private final ObjectMapper json;
    private final HistoryService history;
    private final GraduationSummaryBuilder summaries;
    private final double growthRate;
    public SchoolLifeServiceImpl(SchoolEventDrawRepository draws, SchoolLifeContent content, ObjectMapper json,
            HistoryService history, GraduationSummaryBuilder summaries, @Value("${silvally.growth.school-event-rate:0.25}") double growthRate) {
        this.draws = draws; this.content = content; this.json = json; this.history = history; this.summaries = summaries;
        if (growthRate < 0 || growthRate > 1) throw new IllegalArgumentException("school event growth rate must be 0..1");
        this.growthRate = growthRate;
    }
    private record Offer(SchoolEventDraw draw, Event event) { }
    private static String period(Player p) {
        return p.getYear() + ":" + p.getTurn() + ":" + p.isVacationPending() + ":" + p.getPracticeRemaining();
    }
    private static SplittableRandom random(Player p, String key) {
        return new SplittableRandom(UUID.nameUUIDFromBytes((p.getSeed() + ":" + period(p) + ":" + key)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).getMostSignificantBits());
    }
    private Offer draw(Player p, String kind, java.util.function.Supplier<Event> factory) {
        SchoolEventDraw draw = draws.findByPlayerIdAndPeriodKeyAndDrawKind(p.getId(), period(p), kind)
                .orElseGet(() -> draws.save(new SchoolEventDraw(p.getId(), period(p), kind, json.writeValueAsString(factory.get()))));
        return new Offer(draw, draw.isResolved() ? null : json.readValue(draw.getEventJson(), Event.class));
    }
    private Offer offer(Player p) {
        if (p.getPhase() != Phase.SCHOOL || p.getPendingChoice() != null) return null;
        if (p.isWithdrawalPending()) return draw(p, "WITHDRAWAL", () -> withdrawal());
        int level = p.getStats().getS() >= 90 ? 90 : p.getStats().getS() >= 60 ? 60 : 0;
        boolean crisisHandled = draws.findByPlayerIdAndPeriodKeyAndDrawKind(p.getId(), period(p), "STRESS_90")
                .map(SchoolEventDraw::isResolved).orElse(false);
        if (level > 0 && !(level == 60 && crisisHandled)) {
            Offer stress = draw(p, "STRESS_" + level, () -> support(p, level));
            if (stress.event() != null) return stress;
        }
        if (p.isVacationPending() || p.getPracticeRemaining() > 0 || !p.hasSemesterFocus()) return null;
        return draw(p, "DAILY", () -> daily(p));
    }
    @Override public Optional<EventView> current(Player p) {
        Offer offer = offer(p);
        return offer == null || offer.event() == null ? Optional.empty() : Optional.of(view(offer.event()));
    }
    private EventView view(Event e) {
        return new EventView(e.code(), e.title(), e.situation(), e.required(), e.choices().stream()
                .map(c -> new EventView.Choice(c.code(), c.label(), 0, true, null, c.description())).toList(),
                e.required(), content.source(e.source()));
    }
    @Override public String choose(Player p, String code, String choiceCode, Map<String, Object> evidence) {
        Offer offer = offer(p);
        if (offer == null || offer.event() == null || !code.equals(offer.event().code()))
            throw new BusinessException(ErrorCode.WRONG_STEP, "현재 제공된 사건만 선택할 수 있습니다.");
        Event event = offer.event();
        Choice choice = event.choices().stream().filter(c -> c.code().equals(choiceCode)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "서버가 제시한 선택지가 아닙니다."));
        offer.draw().resolve();
        int stressBefore = p.getStats().getS();
        applyEffect(p, choice.delta());
        String result = "COMPLETED";
        if (choiceCode.equals("CONSIDER_WITHDRAWAL")) {
            p.setWithdrawalPending(true); result = "WITHDRAWAL_CONSIDERED";
        } else if (choiceCode.equals("STAY_ENROLLED")) {
            p.setWithdrawalPending(false); result = "STAY_ENROLLED";
        } else if (choiceCode.equals("CONFIRM_WITHDRAWAL")) {
            p.setWithdrawalPending(false); withdraw(p); result = "SCHOOL_WITHDRAWAL";
        }
        evidence.put("title", event.title()); evidence.put("situation", event.situation());
        evidence.put("choiceLabel", choice.label()); evidence.put("scene", event.situation() + " " + choice.label() + ".");
        evidence.put("ruleType", "GAME_RULE");
        if (event.source() != null) evidence.put("source", content.source(event.source()));
        int actual = p.getStats().getS() - stressBefore;
        evidence.put("stressLoad", Map.of("stressBefore", stressBefore, "stressAfter", p.getStats().getS(), "total", actual,
                "sources", List.of(Map.of("code", choice.code(), "reason", choice.label(), "delta", actual))));
        return result;
    }
    private void applyEffect(Player p, Map<String, Integer> requested) {
        if (requested == null) return;
        var remainders = new LinkedHashMap<String, Double>();
        if (p.getSchoolGrowthRemainders() != null) json.readTree(p.getSchoolGrowthRemainders()).properties()
                .forEach(e -> remainders.put(e.getKey(), e.getValue().asDouble()));
        var effect = new LinkedHashMap<String, Integer>();
        requested.forEach((stat, value) -> {
            if (stat.equals("G")) throw new IllegalArgumentException("School events must not directly change grades");
            if (value > 0 && Set.of("P", "F", "B", "I", "A", "C", "EN").contains(stat)) {
                double amount = value * growthRate + remainders.getOrDefault(stat, 0.0);
                int applied = (int) Math.floor(amount + 1e-9);
                effect.put(stat, applied); remainders.put(stat, amount - applied);
            } else effect.put(stat, value);
        });
        p.setSchoolGrowthRemainders(json.writeValueAsString(remainders));
        p.getStats().apply(effect);
    }
    private void withdraw(Player p) {
        var summary = new LinkedHashMap<>(summaries.build(p,
                new GraduationPolicy.Decision(GraduationPath.UNEMPLOYED, null, "SCHOOL_WITHDRAWAL", false), GraduationPath.UNEMPLOYED));
        summary.put("schoolStatus", "WITHDRAWN"); summary.put("withdrawalYear", p.getYear()); summary.put("withdrawalTurn", p.getTurn());
        summary.put("employed", false); summary.put("requestedPath", "LEAVE_SCHOOL"); summary.put("path", "LEAVE_SCHOOL"); summary.put("nextPath", "LEAVE_SCHOOL");
        summary.put("finalStats", p.getStats().values());
        EndingView ending = new EndingView("SCHOOL_WITHDRAWAL", "잠시 다른 길을 선택했습니다",
                "부담을 나누고 다음 계획을 돌아본 뒤 학교를 떠나기로 했습니다. 지금까지의 배움과 기록을 가지고 다음 길을 준비합니다.",
                "LEAVE_SCHOOL", null, null, summary);
        p.finishGraduation(ending.endingCode(), json.writeValueAsString(ending));
    }
    private static Choice choice(String code, String label, Map<String, Integer> delta) {
        return new Choice(code, label, delta, null, 0, "게임용 선택 효과: " + delta);
    }
    private static Event event(String code, String title, String text, boolean required, String source, Choice... choices) {
        return new Event(code, title, text, required, List.of(choices), source);
    }
    private static final List<Choice> REST = List.of(
            choice("STRESS_REST", "일정을 줄이고 쉬기", Map.of("S", -12, "H", 6)),
            choice("STRESS_WALK", "운동장을 천천히 걷기", Map.of("S", -11, "H", 4)),
            choice("STRESS_SLEEP", "오늘은 일찍 잠들기", Map.of("S", -14, "H", 8)));
    private static final List<Choice> HELP = List.of(
            choice("STRESS_FRIEND", "친구에게 속마음 털어놓기", Map.of("S", -10)),
            choice("STRESS_TEACHER", "선생님과 부담을 나누기", Map.of("S", -16)),
            choice("STRESS_COUNSELOR", "상담 선생님과 일정 정리하기", Map.of("S", -18)),
            choice("STRESS_MENTOR", "멘토에게 막힌 부분 도움받기", Map.of("S", -13)));
    private static final List<Choice> EXTRA = List.of(
            choice("STRESS_HOBBY", "잠깐 취미에 집중하기", Map.of("S", -9)),
            choice("STRESS_PUSH_ON", "이번 마감까지만 버텨 보기", Map.of("S", 5, "H", -3)),
            choice("STRESS_PLAN", "할 일을 줄이고 우선순위 정하기", Map.of("S", -8)));
    private Event support(Player p, int level) {
        var rng = random(p, "stress:" + level);
        var options = List.of(REST.get(rng.nextInt(REST.size())), HELP.get(rng.nextInt(HELP.size())),
                level == 90 ? choice("CONSIDER_WITHDRAWAL", "자퇴를 고민해 보기", Map.of()) : EXTRA.get(rng.nextInt(EXTRA.size())));
        var situations = List.of("시험 범위와 남은 과제를 번갈아 보다가 손이 멈췄습니다.",
                "친구가 오늘 유난히 말이 없다고 말을 건넸습니다.", "실습을 마쳐도 해야 할 일이 떠나지 않습니다.", "팀 일정과 개인 공부가 겹쳤습니다.");
        return new Event(level == 90 ? "SCHOOL_DOUBTS" : "STRESS_SUPPORT",
                level == 90 ? "학교를 계속 다닐 수 있을까" : "잠시 숨을 고를 시간",
                (level == 90 ? "스트레스가 90 이상입니다. " : "") + situations.get(rng.nextInt(situations.size())) + " 부담을 덜 방법을 선택합니다.",
                true, options, null);
    }
    private static Event withdrawal() {
        return event("WITHDRAWAL_DECISION", "자퇴를 결정하기 전에", "지금까지의 학교생활과 다음 계획을 돌아봅니다. 확정 전에는 재학을 이어갈 수 있습니다.", true, null,
                choice("STAY_ENROLLED", "도움을 받으며 학교생활 이어가기", Map.of("S", -25)),
                choice("CONFIRM_WITHDRAWAL", "자퇴를 확정하고 다음 길 준비하기", Map.of()));
    }
    private Event daily(Player p) {
        var rng = random(p, "daily");
        if (rng.nextInt(100) >= 55) return null;
        var pool = new ArrayList<>(content.candidates(p.getYear(), p.getTurn()));
        pool.add(event("DAILY_ROOMMATE", "기숙사 복도에서 들은 한마디", "룸메이트가 잠깐 산책을 가자고 합니다.", false, "DORM",
                choice("ROOM_WALK", "함께 잠깐 걷고 오기", Map.of("S", -5, "C", 1)),
                choice("ROOM_STUDY", "같이 앉아 과제 정리하기", Map.of("P", 1, "S", 2)),
                choice("ROOM_REST", "오늘은 먼저 쉬기", Map.of("H", 4, "S", -4))));
        pool.add(event("DAILY_FEEDBACK", "예상하지 못한 코드 리뷰", "선생님이 작성한 코드에 질문을 남겼습니다.", false, null,
                choice("REVIEW_ASK", "모르는 부분을 질문하기", Map.of("A", 1, "S", -3)),
                choice("REVIEW_RETRY", "혼자 다시 구현해 보기", Map.of("P", 2, "S", 4)),
                choice("REVIEW_NOTE", "내일 확인할 질문을 적기", Map.of("S", -2))));
        pool.add(event("DAILY_FRIEND", "친구가 자리를 내어 주었다", "친구와 요즘 어떻게 지내는지 이야기할 수 있습니다.", false, null,
                choice("FRIEND_TALK", "요즘 고민을 나누기", Map.of("C", 1, "S", -5)),
                choice("FRIEND_HELP", "친구의 오류를 함께 보기", Map.of("P", 1, "C", 1, "S", 1)),
                choice("FRIEND_ALONE", "혼자 조용히 쉬기", Map.of("S", -4, "H", 2))));
        pool.add(event("DAILY_AFTER_CLASS", "실습실 불이 아직 켜져 있다", "방과후 실습실에 몇 명이 남아 있습니다.", false, "AFTER_SCHOOL",
                choice("AFTER_BREAK", "기숙사에서 쉬기", Map.of("H", 5, "S", -5)),
                choice("AFTER_PRACTICE", "작은 기능 하나를 더 구현하기", Map.of("P", 2, "S", 4)),
                choice("AFTER_PLAN", "남은 작업의 우선순위 정하기", Map.of("A", 1, "S", -2))));
        int semesterTurn = (p.getTurn() - 1) % 12 + 1;
        if (Set.of(4, 5, 8, 9, 11).contains(semesterTurn)) pool.add(event("DAILY_EXAM", "시험 준비 방법을 바꿔 볼까", "복습과 제출할 과제가 겹쳤습니다.", false, null,
                choice("EXAM_GROUP", "친구와 모르는 문제 비교하기", Map.of("A", 1, "S", -3)),
                choice("EXAM_CRAM", "오늘 밤 공부 시간을 늘리기", Map.of("A", 2, "S", 5, "H", -3)),
                choice("EXAM_SPLIT", "범위를 나누고 잠깐 쉬기", Map.of("S", -4))));
        if (developing(p)) pool.add(event("DAILY_NARSHA", "나르샤 회의가 길어졌다", "팀이 남은 기능과 시연 범위를 의논합니다.", false, "NARSHA",
                choice("NARSHA_PAIR", "막힌 기능을 나눠 해결하기", Map.of("P", 1, "C", 1, "S", -3)),
                choice("NARSHA_SCOPE", "필수 기능부터 마무리하기", Map.of("S", -5)),
                choice("NARSHA_EXPAND", "욕심났던 기능도 추가하기", Map.of("P", 2, "S", 5))));
        Event selected = pool.get(rng.nextInt(pool.size()));
        var options = new ArrayList<>(selected.choices());
        for (int i = options.size() - 1; i > 0; i--) Collections.swap(options, i, rng.nextInt(i + 1));
        return new Event(selected.code(), selected.title(), selected.situation(), false, List.copyOf(options), selected.source());
    }
    private static boolean developing(Player p) {
        return p.getYear() == 1 && "STARTED".equals(p.getNarshaStage())
                || p.getYear() == 2 && "RESUMED".equals(p.getNarshaStage());
    }
    private static boolean deadline(Player p) {
        return developing(p) && p.getTurn() >= (p.getYear() == 1 ? 20 : 19)
                || p.getYear() == 1 && p.getTurn() == 23 && "WRAPPED".equals(p.getNarshaStage())
                || p.getYear() == 2 && p.getTurn() == 22 && "RANKED".equals(p.getNarshaStage());
    }
    @Override public Map<String, Object> applyTurnPressure(Player p) {
        int before = p.getStats().getS();
        var sources = new ArrayList<Map<String, Object>>();
        var rng = random(p, "pressure");
        pressure(p, sources, "SCHOOL_ROUTINE", "수업과 과제를 이어가며 부담이 조금 쌓였다.", rng.nextInt(2));
        int turn = (p.getTurn() - 1) % 12 + 1;
        switch (turn) {
            case 4, 5 -> pressure(p, sources, "MIDTERM_PREP", "중간고사 복습 범위를 정리했다.", 1 + rng.nextInt(2));
            case 6 -> pressure(p, sources, "MIDTERM", "중간고사를 치르며 긴장했다.", 3 + rng.nextInt(3));
            case 8, 9 -> pressure(p, sources, "ASSESSMENT_PREP", "수행평가 일정과 과제를 맞췄다.", 1);
            case 10 -> pressure(p, sources, "ASSESSMENT", "수행평가 결과물을 제출했다.", 3 + rng.nextInt(2));
            case 11 -> pressure(p, sources, "FINAL_PREP", "기말 준비와 과제가 겹쳤다.", 2 + rng.nextInt(2));
            case 12 -> pressure(p, sources, "FINAL", "기말고사를 치르고 학기를 정리했다.", 4 + rng.nextInt(3));
            default -> { }
        }
        boolean deadline = deadline(p);
        if (developing(p) || deadline) pressure(p, sources, deadline ? "NARSHA_DEADLINE" : "NARSHA_WORK",
                deadline ? "나르샤 발표와 마감이 가까워 시연과 오류를 점검했다." : "나르샤 팀 작업과 수업을 함께 진행했다.",
                deadline ? 2 + rng.nextInt(2) : rng.nextInt(2));
        if (!Set.of(6, 10, 12).contains(turn) && !deadline && p.getStats().getH() >= 40)
            pressure(p, sources, "DAILY_RECOVERY", "일과를 마치고 쉬며 부담을 덜었다.", -2);
        return Map.of("stressBefore", before, "stressAfter", p.getStats().getS(), "total", p.getStats().getS() - before, "sources", sources);
    }
    private static void pressure(Player p, List<Map<String, Object>> sources, String code, String reason, int amount) {
        int accumulated = sources.stream().mapToInt(s -> Math.max(0, (Integer) s.get("delta"))).sum();
        if (amount > 0) amount = Math.min(amount, Math.max(0, 5 - accumulated));
        if (amount == 0) return;
        int before = p.getStats().getS();
        p.getStats().apply(Map.of("S", amount));
        sources.add(Map.of("code", code, "reason", reason, "delta", p.getStats().getS() - before));
    }
    @Override public void recordTurnScene(Player p, Map<String, Object> load) {
        var scheduled = content.scenes().stream().filter(s -> s.year() == p.getYear() && s.turn() == p.getTurn()).findFirst();
        int turn = (p.getTurn() - 1) % 12 + 1;
        if (scheduled.isEmpty() && !Set.of(6, 10, 12).contains(turn) && !deadline(p)) return;
        var evidence = new LinkedHashMap<String, Object>();
        String title = scheduled.map(Scene::title).orElse("시험과 프로젝트의 고비");
        String premise = scheduled.map(Scene::premise).orElse("시험과 마감에 대처한 과정을 돌아보고 다음 일정을 정리했다.");
        evidence.put("title", title); evidence.put("situation", premise); evidence.put("scene", premise);
        scheduled.ifPresent(s -> evidence.put("source", content.source(s.source())));
        evidence.put("stressLoad", load); evidence.put("ruleType", "GAME_RULE");
        history.recordScene(p, new SceneRecord("SCHOOL_TIMELINE_Y" + p.getYear() + "_T" + p.getTurn(), "OBSERVE", "RECORDED", json.writeValueAsString(evidence)));
    }
}
