package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.PlayerView;
import com.silvally.entity.ExamKind;
import com.silvally.entity.Player;
import com.silvally.entity.Stats;
import com.silvally.repository.ExamResultRepository;
import com.silvally.repository.PlayerRepository;
import com.silvally.support.AdmissionAnswers;
import com.silvally.support.TestStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class StatGrowthExamTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ActivityService events;

    @Autowired
    private ExamResultRepository examResults;

    @Autowired
    private SchoolFixture fixture;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private InterviewService interviews;

    @Test
    @DisplayName("같은 생성 요청 재시도와 재조회는 처음 추첨한 초기 능력치를 그대로 돌려준다")
    void create_rollsOnceAndReplaysSameStats() {
        String key = UUID.randomUUID().toString();

        PlayerView first = playerService.create(key, new CreatePlayerRequest("roll", null));
        PlayerView retry = playerService.create(key, new CreatePlayerRequest("roll", null));

        assertThat(retry).isEqualTo(first);
        assertThat(playerService.get(first.id()).stats()).isEqualTo(first.stats());
        Player saved = fixture.load(first.id());
        assertThat(first.stats()).isEqualTo(StatRules.rollInitial(StatRules.statSeed(saved.getSeed())).toMap());
        StatRules.INITIAL_RANGES.forEach((stat, range) ->
                assertThat(first.stats().get(stat)).as(stat).isBetween(range.min(), range.max()));
    }

    @Test
    @DisplayName("1점 미만 성장은 회차에 저장되어 재조회 후에도 이어지고 네 턴이 지나야 1점이 된다")
    void smallGrowth_accumulatesAcrossReloads() {
        UUID id = student(p -> { });
        int p = fixture.load(id).getStats().getP();

        for (int turn = 1; turn <= 3; turn++) {
            assertThat(fixture.advance(id).statDelta()).doesNotContainKey("P");
            assertThat(fixture.load(id).getGrowthProgress().get("P")).isEqualTo(250 * turn);
        }
        MutationResult fourth = fixture.advance(id);

        assertThat(fourth.statDelta()).containsEntry("P", 1);
        assertThat(fixture.load(id).getStats().getP()).isEqualTo(p + 1);
        assertThat(fixture.load(id).getGrowthProgress().get("P")).isZero();
    }

    @Test
    @DisplayName("고능력치는 성장이 감쇠되고 100에 닿으면 더 오르지 않는다")
    void highStats_dampedAndCapped() {
        UUID id = student(p -> p.getStats().apply(Map.of("P", 70, "EN", 100)));

        fixture.advance(id);

        Player player = fixture.load(id);
        assertThat(player.getStats().getP()).isEqualTo(90);
        assertThat(player.getGrowthProgress().get("P")).isEqualTo(63);
        assertThat(player.getStats().getEn()).isEqualTo(100);
        assertThat(player.getGrowthProgress().get("EN")).isZero();
    }

    @Test
    @DisplayName("저컨디션 턴에는 A가 1점 하락하고 사유가 남으며 하한 5 아래로는 내려가지 않는다")
    void lowCondition_limitedDeclineWithReason() {
        UUID id = student(p -> p.getStats().apply(Map.of("H", 20 - p.getStats().getH())));

        MutationResult result = fixture.advance(id);

        assertThat(result.statDelta()).containsEntry("A", -1);
        assertThat(statChanges(result)).contains(Map.of("stat", "A", "delta", -1, "reason", "LOW_CONDITION"));

        UUID floor = student(p -> p.getStats().apply(Map.of("H", 20 - p.getStats().getH(), "A", 5 - p.getStats().getA())));
        fixture.advance(floor);
        assertThat(fixture.load(floor).getStats().getA()).isEqualTo(5);
    }

    @Test
    @DisplayName("학기 동안 기르지 않은 기술만 학기 말에 1점 하락하고, 기말 평가 뒤 방학으로 넘어간다")
    void semesterEnd_neglectDeclineAfterFinalExam() {
        UUID id = student(p -> { });
        fixture.advanceTimes(id, 11);
        Player before = fixture.load(id);

        MutationResult end = fixture.advance(id);

        assertThat(end.resultCode()).isEqualTo("VACATION_REQUIRED");
        assertThat(end.details().get("exam")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("kind", "FINAL").containsEntry("semester", 1);
        assertThat(statChanges(end)).contains(
                Map.of("stat", "F", "delta", -1, "reason", "NEGLECTED"),
                Map.of("stat", "B", "delta", -1, "reason", "NEGLECTED"),
                Map.of("stat", "I", "delta", -1, "reason", "NEGLECTED"));
        assertThat(statChanges(end)).noneMatch(change -> "P".equals(change.get("stat")) && "NEGLECTED".equals(change.get("reason")));
        assertThat(fixture.load(id).getStats().getF()).isEqualTo(before.getStats().getF() - 1);
    }

    @Test
    @DisplayName("일반 턴과 학교 사건에서는 G가 그대로이고, 6·10·12턴 평가에서만 바뀐다")
    void grade_changesOnlyByExams() {
        UUID id = student(p -> { });
        int grade = fixture.load(id).getStats().getG();

        events.chooseEvent(id, "S01", fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest("COOPERATE"));
        Player detached = fixture.load(id);
        assertThat(StatGrowth.apply(detached, Map.of("G", 30), "IGNORED")).isEmpty();
        assertThat(detached.getStats().getG()).isEqualTo(grade);
        for (int turn = 1; turn <= 5; turn++) {
            MutationResult result = fixture.advance(id);
            assertThat(result.statDelta()).doesNotContainKey("G");
            assertThat(result.details()).doesNotContainKey("exam");
        }
        assertThat(fixture.load(id).getStats().getG()).isEqualTo(grade);

        MutationResult midterm = fixture.advance(id);

        @SuppressWarnings("unchecked")
        Map<String, Object> exam = (Map<String, Object>) midterm.details().get("exam");
        assertThat(exam).containsEntry("kind", "MIDTERM").containsEntry("turn", 6).containsEntry("ruleSource", "GAME_RULE");
        int score = (int) exam.get("score");
        assertThat(exam).containsEntry("gradeBefore", grade)
                .containsEntry("gradeAfter", StatRules.nextGrade(grade, score, ExamKind.MIDTERM));
        assertThat(fixture.load(id).getStats().getG()).isEqualTo(exam.get("gradeAfter"));
        assertThat(examResults.findByPlayerIdOrderByIdAsc(id)).singleElement()
                .satisfies(r -> assertThat(r.getKind()).isEqualTo(ExamKind.MIDTERM));
    }

    @Test
    @DisplayName("준비가 잘 된 시험은 G를 올리고, 준비 없이 지친 상태의 시험은 G를 내린다")
    void exam_raisesOrLowersGrade() {
        UUID strong = student(p -> {
            setPeriod(p, 6);
            p.getStats().apply(Map.of("A", 50, "P", 30, "H", 20, "S", -20));
            p.getStats().changeGrade(60);
            p.addExamPreparation(20);
        });
        UUID weak = student(p -> {
            setPeriod(p, 6);
            p.getStats().apply(Map.of("H", 25 - p.getStats().getH(), "S", 90 - p.getStats().getS()));
            p.getStats().changeGrade(90);
            p.spendAp(SchoolRules.AP_PER_TURN);
        });

        fixture.advance(strong);
        fixture.advance(weak);

        assertThat(fixture.load(strong).getStats().getG()).isGreaterThan(60);
        assertThat(fixture.load(weak).getStats().getG()).isLessThan(90);
        assertThat(fixture.load(strong).getExamPreparation()).isZero();
    }

    @Test
    @DisplayName("기말 평가는 캠프 실습 턴과 같은 키 재시도에서 다시 평가되지 않고, 낡은 버전 요청은 기록을 남기지 않는다")
    void finalExam_notReevaluatedByCampRetryOrConflict() {
        UUID id = student(p -> setPeriod(p, 12));
        long version = fixture.version(id);
        String key = UUID.randomUUID().toString();

        MutationResult first = playerService.advanceTurn(id, version, key);
        MutationResult retry = playerService.advanceTurn(id, version, key);
        assertThatThrownBy(() -> playerService.advanceTurn(id, version, UUID.randomUUID().toString()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        int grade = fixture.load(id).getStats().getG();
        events.chooseEvent(id, "VACATION_Y1_SUMMER", fixture.version(id), UUID.randomUUID().toString(),
                new ChooseEventRequest("CAMP"));
        fixture.advance(id);
        MutationResult resumed = fixture.advance(id);

        assertThat(retry).isEqualTo(first);
        assertThat(first.resultCode()).isEqualTo("VACATION_REQUIRED");
        assertThat(resumed.resultCode()).isEqualTo("SEMESTER_ADVANCED");
        assertThat(resumed.details()).doesNotContainKey("exam");
        assertThat(examResults.findByPlayerIdOrderByIdAsc(id)).singleElement()
                .satisfies(r -> assertThat(r.getKind()).isEqualTo(ExamKind.FINAL));
        assertThat(fixture.load(id).getStats().getG()).isEqualTo(grade);
    }

    @Test
    @DisplayName("1학년 한 해를 마치면 학기마다 중간·수행·기말 세 번씩 여섯 번 평가하고 2학년이 된다")
    void fullYear_sixExamsBeforeYearTransition() {
        UUID id = student(p -> { });

        MutationResult yearEnd = fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR);

        assertThat(yearEnd.resultCode()).isEqualTo("YEAR_ADVANCED");
        assertThat(examResults.findByPlayerIdOrderByIdAsc(id))
                .extracting(r -> r.getSemester() + ":" + r.getKind() + "@" + r.getTurn())
                .containsExactly("1:MIDTERM@6", "1:PERFORMANCE@10", "1:FINAL@12",
                        "2:MIDTERM@18", "2:PERFORMANCE@22", "2:FINAL@24");
        assertThat(fixture.load(id).getYear()).isEqualTo(2);

        playerService.delete(id, fixture.version(id));
        assertThat(examResults.findByPlayerIdOrderByIdAsc(id)).isEmpty();
    }

    @ParameterizedTest(name = "초기 범위 {0}")
    @ValueSource(strings = {"MIN", "MAX"})
    @DisplayName("초기 범위 양 끝 능력치에서도 입학 정상 합격·보호막 통과·탈락 경로가 유지된다")
    void admissionPaths_holdAtInitialRangeEdges(String edge) {
        Stats stats = edgeStats("MAX".equals(edge));

        MutationResult passed = submit(prepared(stats, "GENERAL"), AdmissionAnswers.STRONG);
        MutationResult failed = submit(prepared(stats, "GENERAL"), "모르겠습니다.");
        Player shield = Player.admission("보호막", "OPEN");
        ReflectionTestUtils.setField(shield, "seed", "admission-test");
        ReflectionTestUtils.setField(shield, "stats", stats);
        shield.selectAdmissionTrack("MEISTER");
        shield = players.saveAndFlush(shield);
        MutationResult documents = events.chooseEvent(shield.getId(), "AD02", shield.getVersion(),
                UUID.randomUUID().toString(), new ChooseEventRequest("ACADEMIC"));

        assertThat(passed.resultCode()).isEqualTo("PASSED");
        assertThat(failed.resultCode()).isEqualTo("FAILED");
        assertThat(documents.resultCode()).isEqualTo("SHIELD_PASSED");
    }

    private static Stats edgeStats(boolean max) {
        Map<String, Integer> values = new java.util.LinkedHashMap<>();
        StatRules.INITIAL_RANGES.forEach((stat, range) -> values.put(stat, max ? range.max() : range.min()));
        return Stats.of(values);
    }

    private Player prepared(Stats stats, String track) {
        Player p = Player.admission("경계", "OPEN");
        ReflectionTestUtils.setField(p, "seed", "admission-test");
        ReflectionTestUtils.setField(p, "stats", Stats.of(stats.toMap()));
        p.selectAdmissionTrack(track);
        p.passDocuments();
        p.prepare("AD03", 95);
        p.prepare("AD04", 95);
        return players.saveAndFlush(p);
    }

    private MutationResult submit(Player p, String answer) {
        var question = interviews.getSchoolQuestion(p.getId());
        return events.chooseEvent(p.getId(), "AD05", p.getVersion(), UUID.randomUUID().toString(),
                new ChooseEventRequest("ANSWER", null, question.questionId(), answer));
    }

    private UUID student(Consumer<Player> setup) {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> {
            TestStats.pin(p);
            setup.accept(p);
        });
        return id;
    }

    private static void setPeriod(Player player, int turn) {
        ReflectionTestUtils.setField(player, "turn", turn);
        player.chooseSemesterFocus("C");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> statChanges(MutationResult result) {
        return (List<Map<String, Object>>) result.details().getOrDefault("statChanges", List.of());
    }
}
