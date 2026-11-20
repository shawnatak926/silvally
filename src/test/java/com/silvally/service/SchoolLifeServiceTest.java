package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.request.*;
import com.silvally.dto.response.*;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(SchoolFixture.class)
class SchoolLifeServiceTest {
    @Autowired SchoolFixture fixture;
    @Autowired ActivityService events;
    @Autowired PlayerService players;
    @Autowired SchoolTurnService turns;
    @Autowired CareerService careers;
    @Autowired SchoolLifeService life;
    @Autowired SchoolLifeContent content;
    @Autowired SchoolEventDrawRepository draws;
    @Autowired CommandLogRepository logs;
    @Autowired LifeNarrativeRepository narratives;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;

    void at(UUID id, int year, int turn, int stress) {
        fixture.modify(id, p -> {
            ReflectionTestUtils.setField(p, "year", year); ReflectionTestUtils.setField(p, "turn", turn);
            p.chooseSemesterFocus("C"); p.getStats().apply(Map.of("S", stress - p.getStats().getS()));
        });
    }
    EventView stress(UUID id) { return events.getEvents(id).stream().filter(e -> e.code().equals("STRESS_SUPPORT") || e.code().equals("SCHOOL_DOUBTS") || e.code().equals("WITHDRAWAL_DECISION")).findFirst().orElseThrow(); }
    MutationResult choose(UUID id, String code, String option) {
        return events.chooseEvent(id, code, fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest(option));
    }
    @Test void thresholds59_60_89_90AndRequiredTurnBlock() {
        UUID id = fixture.enrolledStudent();
        for (int s : List.of(59, 60, 89, 90)) {
            at(id, 1, 3 + s % 5, s);
            var offered = events.getEvents(id);
            if (s == 59) assertThat(offered).noneMatch(e -> SchoolLifeService.owns(e.code()) && e.required());
            else {
                EventView e = stress(id);
                assertThat(e.code()).isEqualTo(s >= 90 ? "SCHOOL_DOUBTS" : "STRESS_SUPPORT");
                assertThat(e.blocksAuto()).isTrue();
                assertThat(e.choices()).hasSize(3);
                assertThatThrownBy(() -> fixture.advance(id)).isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHOICE_REQUIRED));
            }
        }
    }
    @Test void savedDrawOrderAndVersionSurviveReloadAndKeyReplay() {
        UUID id = fixture.enrolledStudent(); at(id, 1, 3, 75);
        long version = fixture.version(id);
        var first = stress(id);
        assertThat(stress(id)).isEqualTo(first);
        assertThat(events.getEvent(id, first.code())).isEqualTo(first);
        assertThat(fixture.version(id)).isEqualTo(version);
        String key = UUID.randomUUID().toString();
        var request = new ChooseEventRequest(first.choices().getFirst().code());
        var result = events.chooseEvent(id, first.code(), version, key, request);
        assertThat(result.statDelta().get("S")).isNegative();
        assertThat(events.chooseEvent(id, first.code(), version, key, request)).isEqualTo(result);
        assertThat(logs.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, first.code())).hasSize(1);
        assertThatThrownBy(() -> choose(id, first.code(), first.choices().getFirst().code())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> events.chooseEvent(id, first.code(), version, UUID.randomUUID().toString(), request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        assertThatThrownBy(() -> events.chooseEvent(id, first.code(), version, key, new ChooseEventRequest("OTHER")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
    }
    @Test void sixtyThenNinetyInSameTurnIsDistinctButNinetyDoesNotOfferSixtyAgain() {
        UUID id = fixture.enrolledStudent(); at(id, 1, 3, 60);
        EventView support = stress(id); choose(id, support.code(), support.choices().getFirst().code());
        fixture.addStats(id, Map.of("S", 100));
        EventView crisis = stress(id); assertThat(crisis.code()).isEqualTo("SCHOOL_DOUBTS");
        choose(id, crisis.code(), crisis.choices().getFirst().code());
        assertThat(events.getEvents(id)).noneMatch(e -> e.code().equals("STRESS_SUPPORT") || e.code().equals("SCHOOL_DOUBTS"));
    }
    @Test void considerationDoesNotEndSchoolAndStayRestoresProgress() {
        UUID id = fixture.enrolledStudent(); at(id, 1, 3, 90);
        choose(id, "SCHOOL_DOUBTS", "CONSIDER_WITHDRAWAL");
        assertThat(fixture.load(id).getPhase()).isEqualTo(Phase.SCHOOL);
        assertThat(stress(id).code()).isEqualTo("WITHDRAWAL_DECISION");
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOf(BusinessException.class);
        choose(id, "WITHDRAWAL_DECISION", "STAY_ENROLLED");
        assertThat(fixture.load(id).getStats().getS()).isEqualTo(65);
        fixture.advance(id);
        assertThat(fixture.load(id).getTurn()).isEqualTo(4);
    }
    @Test void confirmedWithdrawalStoresEndingAndRejectsFurtherTurns() {
        UUID id = fixture.enrolledStudent(); at(id, 2, 7, 90);
        choose(id, "SCHOOL_DOUBTS", "CONSIDER_WITHDRAWAL");
        var result = choose(id, "WITHDRAWAL_DECISION", "CONFIRM_WITHDRAWAL");
        assertThat(result.player().phase()).isEqualTo("ENDED");
        var ending = careers.getEnding(id);
        assertThat(ending.endingCode()).isEqualTo("SCHOOL_WITHDRAWAL");
        assertThat(ending.summary()).containsEntry("schoolStatus", "WITHDRAWN").containsEntry("withdrawalYear", 2)
                .containsEntry("withdrawalTurn", 7).containsEntry("employed", false).containsKeys("projects", "certificates", "finalStats", "fieldTrips");
        assertThat(ending.acceptedCompany()).isNull();
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WRONG_PHASE));
    }
    @Test void schoolAndTermChoicesHavePriorityOverStressAndDailyEvents() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> { p.nextYear(3); p.requireChoice("MAJOR"); p.getStats().apply(Map.of("S", 100)); });
        assertThat(events.getEvents(id)).isEmpty();
        turns.chooseYearTransition(id, fixture.version(id), UUID.randomUUID().toString(), new YearTransitionRequest("SOFTWARE"));
        assertThat(events.getEvents(id)).extracting(EventView::code).containsExactly("FOCUS_Y2_S1");
        assertThatThrownBy(() -> choose(id, "SCHOOL_DOUBTS", "CONSIDER_WITHDRAWAL")).isInstanceOf(BusinessException.class);
        choose(id, "FOCUS_Y2_S1", "COMMUNICATION");
        assertThat(stress(id).code()).isEqualTo("SCHOOL_DOUBTS");
    }
    @Test void invalidOptionLeavesSnapshotStatsAndHistoryUnchanged() {
        UUID id = fixture.enrolledStudent(); at(id, 1, 3, 65);
        var e = stress(id); var before = fixture.load(id).view();
        assertThatThrownBy(() -> choose(id, e.code(), "MADE_UP")).isInstanceOf(BusinessException.class);
        assertThat(fixture.load(id).view()).isEqualTo(before);
        assertThat(stress(id)).isEqualTo(e);
        assertThat(logs.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, e.code())).isEmpty();
    }
    @Test void dailyEventsVaryAcrossSeedsAndRemainOptionalAndEligible() {
        Set<String> seen = new HashSet<>(); Set<List<String>> stressOrders = new HashSet<>();
        for (int i = 0; i < 36; i++) {
            UUID id = fixture.enrolledStudent(); at(id, 3, 14, 20);
            for (EventView e : events.getEvents(id)) {
                if (!e.code().startsWith("DAILY_")) continue;
                assertThat(e.required()).isFalse(); assertThat(e.blocksAuto()).isFalse(); seen.add(e.code());
                assertThat(e.code()).isNotEqualTo("DAILY_PRODUCT");
                assertThat(events.getEvent(id, e.code())).isEqualTo(e);
            }
            fixture.advance(id); // Optional daily scenes never block time.
            at(id, 3, 16, 65);
            var e = stress(id); stressOrders.add(e.choices().stream().map(EventView.Choice::code).toList());
            assertThat(e.choices().get(0).code()).startsWith("STRESS_");
            assertThat(e.choices().get(1).code()).isIn("STRESS_FRIEND", "STRESS_TEACHER", "STRESS_COUNSELOR", "STRESS_MENTOR");
        }
        assertThat(seen.size()).isGreaterThan(3); assertThat(stressOrders.size()).isGreaterThan(3);
    }
    @Test void allThirteenSchoolActivitiesApplyAndRecordEvidenceWithoutCreatingProgramResults() {
        assertThat(content.activities()).hasSize(13); assertThat(content.scenes()).hasSize(20); assertThat(content.sources()).hasSize(35);
        for (var activity : content.activities()) {
            UUID id = fixture.enrolledStudent(); at(id, activity.years().getFirst(), activity.minTurn(), 20);
            var p = fixture.load(id); var event = activity.event();
            draws.saveAndFlush(new SchoolEventDraw(id, p.getYear() + ":" + p.getTurn() + ":false:0", "DAILY", json.writeValueAsString(event)));
            var choice = event.choices().getFirst();
            var result = choose(id, event.code(), choice.code());
            assertThat(result.spentAp()).isZero(); assertThat(result.details()).containsKeys("source", "statDelta", "stressLoad");
            assertThat(result.statDelta()).doesNotContainKey("G");
            var history = players.getHistory(id, 0, 50).items().stream().filter(item -> item.subjectCode().equals(event.code())).findFirst().orElseThrow();
            assertThat(history.narrative()).isNotBlank(); assertThat(history.evidenceJson()).contains(event.title(), "GAME_RULE");
            assertThat(fixture.load(id).getEndingCode()).isNull();
            assertThatThrownBy(() -> choose(id, event.code(), choice.code())).isInstanceOf(BusinessException.class);
        }
    }
    @Test void fractionalGrowthAccumulatesAcrossPeriodsInsteadOfRoundingEachRewardUp() {
        UUID id = fixture.enrolledStudent(); int initial = fixture.load(id).getStats().getA();
        var activity = content.activities().stream().filter(a -> a.event().code().equals("DAILY_REFLECTION")).findFirst().orElseThrow();
        for (int turn = 2; turn <= 5; turn++) {
            at(id, 1, turn, 20);
            draws.saveAndFlush(new SchoolEventDraw(id, "1:" + turn + ":false:0", "DAILY", json.writeValueAsString(activity.event())));
            choose(id, "DAILY_REFLECTION", "REFLECT_NOTE");
            assertThat(fixture.load(id).getStats().getA()).isEqualTo(initial + (turn - 1) / 4);
        }
    }
    @Test void timelineUsesSavedNarrativeAndIdempotentCommandsAndHistoryIncludesSignedPressure() {
        UUID id = fixture.enrolledStudent(); long version = fixture.version(id); String key = UUID.randomUUID().toString();
        var result = players.advanceTurn(id, version, key);
        assertThat(players.advanceTurn(id, version, key)).isEqualTo(result);
        var scenes = logs.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "SCHOOL_TIMELINE_Y1_T1");
        assertThat(scenes).hasSize(1);
        var log = scenes.getFirst();
        assertThat(log.getEvidenceJson()).contains("조회에서 기숙사 점호까지", "stressLoad", "DAILY_RECOVERY");
        assertThat(narratives.findByCommandLogId(log.getId()).orElseThrow().getText()).contains("조회에서 기숙사 점호까지");
        assertThat(players.getHistory(id, 0, 50).items()).anyMatch(item -> item.subjectCode().equals("TURN_END") && item.evidenceJson().contains("stressLoad"));
    }
    @Test void regularTurnPressureCapsAtFiveAndReplayDoesNotReapplyIt() {
        UUID id = fixture.enrolledStudent(); at(id, 1, 22, 20); fixture.modify(id, p -> p.setNarshaStage("STARTED"));
        long version = fixture.version(id); String key = UUID.randomUUID().toString();
        var result = players.advanceTurn(id, version, key);
        assertThat(result.statDelta().get("S")).isBetween(3, 5);
        assertThat(players.advanceTurn(id, version, key)).isEqualTo(result);
        assertThat(fixture.load(id).getStats().getS()).isEqualTo(20 + result.statDelta().get("S"));
        assertThat(result.details().get("stressLoad").toString()).contains("ASSESSMENT");
    }
    @Test void allScheduledScenesAreRecordedWithSourcesOnceAndWithoutFakeParticipation() {
        UUID id = fixture.enrolledStudent();
        for (var scene : content.scenes()) {
            at(id, scene.year(), scene.turn(), 20);
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                // Two identical requests must still create one scene/narrative.
                var p = fixture.load(id); var load = Map.<String, Object>of("total", 0);
                life.recordTurnScene(p, load); life.recordTurnScene(p, load);
            });
            var saved = logs.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "SCHOOL_TIMELINE_Y" + scene.year() + "_T" + scene.turn());
            assertThat(saved).hasSize(1); assertThat(saved.getFirst().getEvidenceJson()).contains("source", scene.title());
        }
        assertThat(fixture.load(id).getEndingCode()).isNull();
    }
    @Test void full72RegularTurnsWithNarshaAndSupportReachGraduation() {
        UUID id = fixture.enrolledStudent(); int regular = 0;
        while (fixture.load(id).getPhase() == Phase.SCHOOL && regular < 73) {
            Player p = fixture.load(id);
            if (p.getPendingChoice() != null) turns.chooseYearTransition(id, p.getVersion(), UUID.randomUUID().toString(),
                    new YearTransitionRequest(p.getYear() == 2 ? "SOFTWARE" : "WEB"));
            fixture.completeCalendarChoices(id);
            p = fixture.load(id); if (p.getPhase() != Phase.SCHOOL) break;
            if (p.getYear() == 1 && p.getTurn() == 8) events.chooseEvent(id, "NARSHA_START", fixture.version(id), UUID.randomUUID().toString(),
                    com.silvally.dto.request.ChooseEventRequest.withNarsha("JOIN", new com.silvally.dto.request.NarshaInput("CUSTOM", "테스트 프로젝트", null)));
            fixture.advance(id); regular++;
            fixture.completeCalendarChoices(id);
        }
        assertThat(regular).isEqualTo(72); assertThat(fixture.load(id).getPhase()).isEqualTo(Phase.GRAD_READY);
    }
}
