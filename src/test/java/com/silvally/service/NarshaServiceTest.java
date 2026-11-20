package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.NarshaActionRequest;
import com.silvally.dto.request.NarshaInput;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.NarshaView;
import com.silvally.entity.NarshaProject;
import com.silvally.entity.NarshaStatus;
import com.silvally.repository.NarshaProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SchoolFixture.class)
class NarshaServiceTest {

    @Autowired
    private SchoolFixture fixture;
    @Autowired
    private ActivityService events;
    @Autowired
    private NarshaService narsha;
    @Autowired
    private NarshaProjectRepository repository;
    @Autowired
    private PlayerService players;
    @Autowired
    private GraduationSummaryBuilder summaries;

    @Test
    @DisplayName("1학년 완료 후 2학년을 시작하면 두 학년 기록이 각각 남는다")
    void twoYears_keepSeparateRecords() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        MutationResult joined = start(id, "NARSHA_START", "JOIN", new NarshaInput("EXPENSE_PLATFORM", null, "PLANNER"));
        assertThat(joined.details()).containsKey("narshaProject");
        at(id, 1, 23);
        MutationResult wrapped = choose(id, "NARSHA_WRAP", "SUBMIT");
        int firstQuality = (int) wrapped.details().get("quality");

        at(id, 2, 2);
        start(id, "NARSHA_RESUME", "RESUME", new NarshaInput("CUSTOM", "  실시간 팀 일정 조율 서비스 ", "BACKEND"));

        List<NarshaProject> rows = repository.findByPlayerIdOrderByYearAsc(id);
        assertThat(rows).extracting(NarshaProject::getYear).containsExactly(1, 2);
        NarshaProject first = rows.get(0);
        assertThat(first.getStatus()).isEqualTo(NarshaStatus.WRAPPED);
        assertThat(first.getTopic()).isEqualTo("개인·모임 지출 관리 플랫폼");
        assertThat(first.getRole()).isEqualTo("PLANNER");
        assertThat(first.getQuality()).isEqualTo(firstQuality);
        assertThat(first.getRank()).isNull();
        NarshaProject second = rows.get(1);
        assertThat(second.getStatus()).isEqualTo(NarshaStatus.IN_PROGRESS);
        assertThat(second.getTopic()).isEqualTo("실시간 팀 일정 조율 서비스");
        assertThat(second.getTopicSource()).isEqualTo(NarshaProject.SOURCE_CUSTOM);
        assertThat(second.getStartTurn()).isEqualTo(2);
        assertThat(fixture.load(id).view().school().narshaStage()).isEqualTo("RESUMED");
    }

    @Test
    @DisplayName("1학년에 참가하지 않아도 2학년에 새로 시작할 수 있고 1학년 기록은 만들지 않는다")
    void year2Start_withoutYear1Participation() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        choose(id, "NARSHA_START", "SKIP");
        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).isEmpty();

        at(id, 2, 2);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).contains("NARSHA_RESUME");
        start(id, "NARSHA_RESUME", "RESUME", new NarshaInput("AI_CALL_TRAINING", null, null));

        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).singleElement().satisfies(row -> {
            assertThat(row.getYear()).isEqualTo(2);
            assertThat(row.getRole()).isEqualTo(NarshaRules.DEFAULT_ROLE);
        });
    }

    @Test
    @DisplayName("2학년 시작을 건너뛰면 기록 없이 넘어가고 순위 발표도 열리지 않는다")
    void year2Skip_noRankEvent() {
        UUID id = fixture.enrolledStudent();
        at(id, 2, 2);
        choose(id, "NARSHA_RESUME", "SKIP");

        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).isEmpty();
        at(id, 2, 22);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).doesNotContain("NARSHA_RANK");
        assertError(() -> choose(id, "NARSHA_RANK", "CHECK"), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("주제 입력은 빈 값·1자·81자를 거절하고 2자·80자는 받는다")
    void topicInput_boundaries() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        assertError(() -> startCustom(id, ""), ErrorCode.VALIDATION_ERROR);
        assertError(() -> startCustom(id, "   "), ErrorCode.VALIDATION_ERROR);
        assertError(() -> startCustom(id, "가"), ErrorCode.VALIDATION_ERROR);
        assertError(() -> startCustom(id, "가".repeat(81)), ErrorCode.VALIDATION_ERROR);
        assertError(() -> start(id, "NARSHA_START", "JOIN", null), ErrorCode.VALIDATION_ERROR);
        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).isEmpty();
        assertThat(events.getEvents(id)).extracting(e -> e.code()).contains("NARSHA_START");

        startCustom(id, "가".repeat(80));
        assertThat(repository.findByPlayerIdAndYear(id, 1).orElseThrow().getTopic()).hasSize(80);

        UUID other = fixture.enrolledStudent();
        at(other, 1, 8);
        startCustom(other, "가나");
        assertThat(repository.findByPlayerIdAndYear(other, 1).orElseThrow().getTopic()).isEqualTo("가나");
    }

    @Test
    @DisplayName("잘못된 선택·시점·대상에 보낸 주제는 거절하고 상태를 바꾸지 않는다")
    void topicInput_wrongChoiceOrEvent() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        long version = fixture.version(id);
        assertError(() -> start(id, "NARSHA_START", "SKIP", new NarshaInput("CUSTOM", "건너뛰는데 보낸 주제", null)),
                ErrorCode.VALIDATION_ERROR);
        assertError(() -> start(id, "NARSHA_START", "JOIN", new NarshaInput("UNKNOWN_TOPIC", null, null)),
                ErrorCode.VALIDATION_ERROR);
        assertError(() -> start(id, "NARSHA_START", "JOIN", new NarshaInput("EXPENSE_PLATFORM", "같이 보냄", null)),
                ErrorCode.VALIDATION_ERROR);
        assertError(() -> start(id, "NARSHA_START", "JOIN", new NarshaInput("EXPENSE_PLATFORM", null, "KING")),
                ErrorCode.VALIDATION_ERROR);
        assertError(() -> start(id, "NARSHA_WRAP", "SUBMIT", new NarshaInput("CUSTOM", "마무리에 보낸 주제", null)),
                ErrorCode.VALIDATION_ERROR);
        assertError(() -> start(id, "FOCUS_Y1_S1", "PROGRAMMING", new NarshaInput("CUSTOM", "학기 목표에 보낸 주제", null)),
                ErrorCode.VALIDATION_ERROR);
        assertThat(fixture.version(id)).isEqualTo(version);
        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).isEmpty();

        // 시작 시점이 아니면 올바른 주제여도 시작할 수 없다.
        at(id, 1, 9);
        assertError(() -> start(id, "NARSHA_START", "JOIN", new NarshaInput("EXPENSE_PLATFORM", null, null)),
                ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("같은 멱등키 재요청은 저장된 응답을 돌려주고 새 키로 다시 시작하면 거절한다")
    void duplicateStart_replayAndRejected() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        long version = fixture.version(id);
        String key = UUID.randomUUID().toString();
        ChooseEventRequest request = ChooseEventRequest.withNarsha("JOIN", new NarshaInput("TRAVEL_PLATFORM", null, "DESIGN"));

        MutationResult first = events.chooseEvent(id, "NARSHA_START", version, key, request);
        MutationResult replay = events.chooseEvent(id, "NARSHA_START", version, key, request);

        assertThat(replay.player()).isEqualTo(first.player());
        assertThat(replay.resultCode()).isEqualTo(first.resultCode());
        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).hasSize(1);
        assertError(() -> events.chooseEvent(id, "NARSHA_START", version, key,
                ChooseEventRequest.withNarsha("JOIN", new NarshaInput("TRAVEL_PLATFORM", null, "BACKEND"))),
                ErrorCode.IDEMPOTENCY_CONFLICT);
        assertError(() -> start(id, "NARSHA_START", "JOIN", new NarshaInput("TRAVEL_PLATFORM", null, "DESIGN")),
                ErrorCode.WRONG_STEP);
        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).hasSize(1);
    }

    @Test
    @DisplayName("나르샤 일정은 1학년 8턴 시작·23턴 마무리, 2학년 2턴 시작·22턴 순위 발표다")
    void schedule_8_23_2_22() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 7);
        assertThat(codes(id)).doesNotContain("NARSHA_START");
        at(id, 1, 8);
        assertThat(codes(id)).contains("NARSHA_START");
        startCustom(id, "일정 확인용 프로젝트");

        at(id, 1, 22);
        assertThat(codes(id)).doesNotContain("NARSHA_WRAP");
        at(id, 1, 23);
        assertThat(codes(id)).contains("NARSHA_WRAP");
        choose(id, "NARSHA_WRAP", "SUBMIT");

        at(id, 2, 1);
        assertThat(codes(id)).doesNotContain("NARSHA_RESUME");
        at(id, 2, 2);
        assertThat(codes(id)).contains("NARSHA_RESUME");
        start(id, "NARSHA_RESUME", "RESUME", new NarshaInput("CAMPUS_COMMUNITY", null, null));

        at(id, 2, 21);
        assertThat(codes(id)).doesNotContain("NARSHA_RANK");
        at(id, 2, 22);
        assertThat(codes(id)).contains("NARSHA_RANK");
    }

    @Test
    @DisplayName("1·3등은 소프트웨이브에 한 번 참가할 수 있고 4등은 참가할 수 없다")
    void exhibit_topThreeOnceAndFourthBlocked() {
        UUID first = rankedStudent(91);
        assertThat(repository.findByPlayerIdAndYear(first, 2).orElseThrow().getRank()).isEqualTo(1);
        at(first, 2, 23);
        assertThat(codes(first)).contains("SOFTWAREWAVE");
        MutationResult attended = choose(first, "SOFTWAREWAVE", "ATTEND");
        assertThat(attended.details()).containsKey("narshaProject");
        assertThat(repository.findByPlayerIdAndYear(first, 2).orElseThrow().isExhibited()).isTrue();
        assertThat(fixture.load(first).isSoftwarewaveAttended()).isTrue();
        assertError(() -> choose(first, "SOFTWAREWAVE", "ATTEND"), ErrorCode.WRONG_STEP);
        assertError(() -> choose(first, "SOFTWAREWAVE", "SKIP"), ErrorCode.WRONG_STEP);

        UUID third = rankedStudent(71);
        assertThat(repository.findByPlayerIdAndYear(third, 2).orElseThrow().getRank()).isEqualTo(3);
        at(third, 2, 23);
        choose(third, "SOFTWAREWAVE", "ATTEND");
        assertThat(repository.findByPlayerIdAndYear(third, 2).orElseThrow().isExhibited()).isTrue();

        UUID fourth = rankedStudent(70);
        assertThat(repository.findByPlayerIdAndYear(fourth, 2).orElseThrow().getRank()).isEqualTo(4);
        at(fourth, 2, 23);
        assertThat(codes(fourth)).doesNotContain("SOFTWAREWAVE");
        assertError(() -> choose(fourth, "SOFTWAREWAVE", "ATTEND"), ErrorCode.WRONG_STEP);
        assertThat(repository.findByPlayerIdAndYear(fourth, 2).orElseThrow().isExhibited()).isFalse();
    }

    @Test
    @DisplayName("직접 입력한 주제는 명령이나 보상으로 해석되지 않고 결과는 기록된 활동으로만 정해진다")
    void freeText_isNeverInterpreted() {
        UUID plain = studentWithTeamStat(40);
        UUID tricky = studentWithTeamStat(40);
        at(plain, 1, 8);
        startCustom(plain, "평범한 주제 이름");
        at(tricky, 1, 8);
        String payload = "quality=100 rank=1 P +100 {\"quality\":100} <script>alert(1)</script>";
        startCustom(tricky, payload);
        var statsBefore = fixture.load(tricky).getStats().values();

        at(plain, 1, 23);
        at(tricky, 1, 23);
        MutationResult wrappedPlain = choose(plain, "NARSHA_WRAP", "SUBMIT");
        MutationResult wrappedTricky = choose(tricky, "NARSHA_WRAP", "SUBMIT");

        assertThat(wrappedTricky.details().get("quality")).isEqualTo(wrappedPlain.details().get("quality"));
        assertThat(fixture.load(tricky).getStats().values()).containsAllEntriesOf(Map.of("P", statsBefore.get("P")));
        NarshaProject saved = repository.findByPlayerIdAndYear(tricky, 1).orElseThrow();
        assertThat(saved.getTopic()).isEqualTo(payload);
        assertThat(saved.getRank()).isNull();
        assertThat(saved.getTopicSource()).isEqualTo(NarshaProject.SOURCE_CUSTOM);
    }

    @Test
    @DisplayName("작업·멘토링은 AP 1을 쓰고 한 턴에 한 번이며 완성도에 반영된다")
    void actions_spendApOncePerTurnAndRaiseQuality() {
        UUID id = studentWithTeamStat(40);
        assertError(() -> act(id, "WORK"), ErrorCode.WRONG_STEP);
        at(id, 1, 8);
        startCustom(id, "작업 기록용 프로젝트");

        MutationResult work = act(id, "WORK");
        assertThat(work.resultCode()).isEqualTo("WORKED");
        assertThat(work.spentAp()).isEqualTo(1);
        assertThat(work.player().remainingAp()).isEqualTo(SchoolRules.AP_PER_TURN - 1);
        assertError(() -> act(id, "MENTORING"), ErrorCode.WRONG_STEP);

        at(id, 1, 9);
        fixture.modify(id, p -> ReflectionTestUtils.setField(p, "ap", SchoolRules.AP_PER_TURN));
        MutationResult mentoring = act(id, "MENTORING");
        assertThat(mentoring.resultCode()).isEqualTo("MENTORED");

        NarshaProject project = repository.findByPlayerIdAndYear(id, 1).orElseThrow();
        assertThat(project.getWorkCount()).isEqualTo(1);
        assertThat(project.getMentoringCount()).isEqualTo(1);
        at(id, 1, 23);
        MutationResult wrapped = choose(id, "NARSHA_WRAP", "SUBMIT");
        int teamStat = (fixture.load(id).getStats().getP() + fixture.load(id).getStats().getB()
                + fixture.load(id).getStats().getC()) / 3;
        assertThat(wrapped.details().get("quality")).isEqualTo(teamStat + 2 + 3);
        assertError(() -> act(id, "WORK"), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("작업 요청은 잘못된 종류·AP 부족·같은 키 재시도를 구분한다")
    void actions_validationApAndReplay() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        startCustom(id, "검증용 프로젝트");
        assertError(() -> narsha.act(id, new NarshaActionRequest("DANCE"), fixture.version(id), UUID.randomUUID().toString()),
                ErrorCode.VALIDATION_ERROR);
        assertError(() -> narsha.act(id, null, fixture.version(id), UUID.randomUUID().toString()), ErrorCode.VALIDATION_ERROR);

        long version = fixture.version(id);
        String key = UUID.randomUUID().toString();
        MutationResult first = narsha.act(id, new NarshaActionRequest("WORK"), version, key);
        assertThat(narsha.act(id, new NarshaActionRequest("WORK"), version, key)).isEqualTo(first);
        assertThat(repository.findByPlayerIdAndYear(id, 1).orElseThrow().getWorkCount()).isEqualTo(1);

        at(id, 1, 9);
        fixture.modify(id, p -> ReflectionTestUtils.setField(p, "ap", 0));
        assertError(() -> act(id, "WORK"), ErrorCode.INSUFFICIENT_AP);
        assertThat(repository.findByPlayerIdAndYear(id, 1).orElseThrow().getWorkCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("진행 현황·인생 기록·졸업 요약에서 두 학년 프로젝트를 모두 조회할 수 있다")
    void progressHistoryAndGraduationSummary_showBothProjects() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        start(id, "NARSHA_START", "JOIN", new NarshaInput("INTERVIEW_PLATFORM", null, "AI_DATA"));
        at(id, 1, 23);
        choose(id, "NARSHA_WRAP", "SUBMIT");
        at(id, 2, 2);
        start(id, "NARSHA_RESUME", "RESUME", new NarshaInput("CUSTOM", "학교 도서관 좌석 알림", "FRONTEND"));
        fixture.addStats(id, Map.of("P", 90, "B", 90, "C", 90));
        at(id, 2, 22);
        choose(id, "NARSHA_RANK", "CHECK");
        at(id, 2, 23);
        choose(id, "SOFTWAREWAVE", "ATTEND");

        List<Map<String, Object>> progress = players.getProgress(id).narshaProjects();
        assertThat(progress).extracting(row -> row.get("year")).containsExactly(1, 2);
        assertThat(progress.get(0)).containsEntry("topic", "IT 면접 연습 플랫폼").containsEntry("status", "WRAPPED")
                .containsEntry("exhibited", false);
        assertThat(progress.get(1)).containsEntry("topic", "학교 도서관 좌석 알림").containsEntry("status", "RANKED")
                .containsEntry("rankSource", "GAME_RULE").containsEntry("exhibited", true);
        assertThat(progress.get(1).get("rank")).isNotNull();

        var history = players.getHistory(id, 0, 100).items();
        assertThat(history).anyMatch(item -> "NARSHA_START".equals(item.subjectCode())
                && item.evidenceJson().contains("IT 면접 연습 플랫폼"));
        assertThat(history).anyMatch(item -> "NARSHA_RESUME".equals(item.subjectCode())
                && item.evidenceJson().contains("학교 도서관 좌석 알림"));
        assertThat(history).anyMatch(item -> "NARSHA_RANK".equals(item.subjectCode()) && item.evidenceJson().contains("rank"));

        Map<String, Object> summary = summaries.build(fixture.load(id),
                new GraduationPolicy.Decision(GraduationPath.UNEMPLOYED, null, "UNEMPLOYED", false), GraduationPath.UNEMPLOYED);
        assertThat(summary.get("narshaProjects")).isEqualTo(progress);

        NarshaView view = narsha.getNarsha(id);
        assertThat(view.topics()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(view.roles()).extracting(NarshaView.Role::code).contains("BACKEND", NarshaRules.DEFAULT_ROLE);
        assertThat(view.projects()).hasSize(2);
        assertThat(view.current()).containsEntry("year", 2);
        assertThat(view.notice()).contains("가상 규칙");
    }

    @Test
    @DisplayName("프로젝트 행이 없는 기존 저장 단계는 처음 마무리할 때 LEGACY 기록으로 변환한다")
    void legacyStage_isConvertedOnFirstMutation() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> p.setNarshaStage("STARTED"));
        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).isEmpty();
        at(id, 1, 23);

        choose(id, "NARSHA_WRAP", "SUBMIT");

        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).singleElement().satisfies(row -> {
            assertThat(row.getTopicSource()).isEqualTo(NarshaProject.SOURCE_LEGACY);
            assertThat(row.getStatus()).isEqualTo(NarshaStatus.WRAPPED);
        });

        UUID second = fixture.enrolledStudent();
        fixture.modify(second, p -> p.setNarshaStage("RESUMED"));
        at(second, 2, 22);
        choose(second, "NARSHA_RANK", "CHECK");
        assertThat(repository.findByPlayerIdOrderByYearAsc(second)).extracting(NarshaProject::getYear).containsExactly(1, 2);
        assertThat(repository.findByPlayerIdOrderByYearAsc(second)).allMatch(row -> NarshaProject.SOURCE_LEGACY.equals(row.getTopicSource()));
        assertThat(repository.findByPlayerIdAndYear(second, 2).orElseThrow().getStatus()).isEqualTo(NarshaStatus.RANKED);
    }

    @Test
    @DisplayName("스트레스 계산에는 현재 학년 프로젝트의 진행·마감 상태가 반영된다")
    void stressContext_followsCurrentYearProject() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        assertThat(NarshaRules.developing(fixture.load(id))).isFalse();
        startCustom(id, "스트레스 확인용 프로젝트");
        at(id, 1, 12);
        assertThat(NarshaRules.developing(fixture.load(id))).isTrue();
        assertThat(NarshaRules.deadline(fixture.load(id))).isFalse();
        at(id, 1, 20);
        assertThat(NarshaRules.deadline(fixture.load(id))).isTrue();
        at(id, 1, 23);
        choose(id, "NARSHA_WRAP", "SUBMIT");
        assertThat(NarshaRules.developing(fixture.load(id))).isFalse();
        assertThat(NarshaRules.deadline(fixture.load(id))).isTrue();
        at(id, 2, 1);
        assertThat(NarshaRules.deadline(fixture.load(id))).isFalse();
    }

    @Test
    @DisplayName("회차를 삭제하면 나르샤 기록도 함께 지운다")
    void delete_removesNarshaRecords() {
        UUID id = fixture.enrolledStudent();
        at(id, 1, 8);
        startCustom(id, "삭제 확인용 프로젝트");
        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).hasSize(1);

        players.delete(id, fixture.version(id));

        assertThat(repository.findByPlayerIdOrderByYearAsc(id)).isEmpty();
    }

    private UUID studentWithTeamStat(int target) {
        UUID id = fixture.enrolledStudent();
        var stats = fixture.load(id).getStats();
        fixture.addStats(id, Map.of("P", target - stats.getP(), "B", target - stats.getB(), "C", target - stats.getC()));
        return id;
    }

    /** 활동 없이 팀 능력치만으로 목표 완성도가 되도록 맞춘 뒤 2학년 순위 발표까지 마친 회차. */
    private UUID rankedStudent(int quality) {
        UUID id = studentWithTeamStat(quality);
        at(id, 2, 2);
        start(id, "NARSHA_RESUME", "RESUME", new NarshaInput("PRIVACY_VISION", null, "AI_DATA"));
        at(id, 2, 22);
        MutationResult result = choose(id, "NARSHA_RANK", "CHECK");
        assertThat(result.details()).containsEntry("quality", quality).containsEntry("rankSource", "GAME_RULE");
        return id;
    }

    private MutationResult act(UUID id, String kind) {
        return narsha.act(id, new NarshaActionRequest(kind), fixture.version(id), UUID.randomUUID().toString());
    }

    private MutationResult startCustom(UUID id, String topic) {
        return start(id, "NARSHA_START", "JOIN", new NarshaInput("CUSTOM", topic, null));
    }

    private MutationResult start(UUID id, String event, String choice, NarshaInput input) {
        ChooseEventRequest request = input == null ? new ChooseEventRequest(choice) : ChooseEventRequest.withNarsha(choice, input);
        return events.chooseEvent(id, event, fixture.version(id), UUID.randomUUID().toString(), request);
    }

    private MutationResult choose(UUID id, String event, String choice) {
        return events.chooseEvent(id, event, fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest(choice));
    }

    private List<String> codes(UUID id) {
        return events.getEvents(id).stream().map(e -> e.code()).toList();
    }

    private void at(UUID id, int year, int turn) {
        fixture.modify(id, p -> {
            ReflectionTestUtils.setField(p, "year", year);
            ReflectionTestUtils.setField(p, "turn", turn);
            p.chooseSemesterFocus("C");
        });
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
