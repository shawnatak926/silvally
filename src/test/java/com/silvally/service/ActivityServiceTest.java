package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ActivityServiceTest {
    @Autowired ActivityService service;
    @Autowired PlayerRepository players;
    @Autowired ActivityRecordRepository records;
    @Autowired CommandLogRepository history;
    @Autowired EventCommandRepository commands;

    Player player() {
        Player p = Player.admission("김일강", "OPEN");
        p.finishAdmission(true);
        return players.saveAndFlush(p);
    }
    MutationResult choose(Player p, String event, ChooseEventRequest request) {
        return service.chooseEvent(p.getId(), event, p.getVersion(), UUID.randomUUID().toString(), request);
    }
    ChooseEventRequest multi(String... codes) { return new ChooseEventRequest(null, Arrays.asList(codes)); }
    void invalidAndUnchanged(Player p, String event, ChooseEventRequest request) {
        long recordCount = records.findByPlayerIdOrderByIdAsc(p.getId()).size();
        long commandCount = commands.count();
        var before = players.findById(p.getId()).orElseThrow().view();
        assertThatThrownBy(() -> choose(p, event, request)).isInstanceOf(BusinessException.class);
        assertThat(players.findById(p.getId()).orElseThrow().view()).isEqualTo(before);
        assertThat(records.findByPlayerIdOrderByIdAsc(p.getId())).hasSize((int) recordCount);
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), event)).isEmpty();
        assertThat(commands.count()).isEqualTo(commandCount);
    }
    @Test void dayOneValidatesEntireRequestBeforeApplyingAnyActivity() {
        Player p = player();
        invalidAndUnchanged(p, "BS01", multi());
        invalidAndUnchanged(p, "BS01", multi("LAB"));
        invalidAndUnchanged(p, "BS01", multi("LAB", "LAB"));
        invalidAndUnchanged(p, "BS01", multi("LAB", "UNKNOWN"));
        invalidAndUnchanged(p, "BS01", multi("LAB", "CLASSROOM", "DORMITORY"));
        invalidAndUnchanged(p, "BS01", multi("LAB", null));
        invalidAndUnchanged(p, "BS01", multi("LAB", " "));
        invalidAndUnchanged(p, "BS01", new ChooseEventRequest("LAB", List.of("CLASSROOM")));
        invalidAndUnchanged(p, "BS01", new ChooseEventRequest("LAB"));
        assertThat(service.getEvent(p.getId(), "BS01").choices()).hasSize(7);
        var result = choose(p, "BS01", multi("LAB", "PROJECT_ROOM"));
        assertThat(result.player().beforeSchoolDay()).isEqualTo(2);
        assertThat(result.statDelta()).containsEntry("P", 5).containsEntry("A", 1).containsEntry("C", 2);
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), "BS01"))
                .extracting(ActivityRecord::getChoiceCode).containsExactly("LAB", "PROJECT_ROOM");
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), "BS01"))
                .hasSize(2).allSatisfy(log -> {
                    assertThat(log.getBeforeSchoolDay()).isEqualTo(1);
                    assertThat(log.getEvidenceJson()).contains("scene", "statDelta", "tags");
                });
    }
    @Test void dayTwoRequiresTwoDifferentValidExperiences() {
        Player p = player(); p.nextBeforeSchoolDay(); p = players.saveAndFlush(p);
        assertThat(service.getEvent(p.getId(), "BS02").choices()).hasSize(8);
        invalidAndUnchanged(p, "BS02", multi("BACKEND"));
        invalidAndUnchanged(p, "BS02", multi("BACKEND", "BACKEND"));
        invalidAndUnchanged(p, "BS02", multi("BACKEND", "UNKNOWN"));
        invalidAndUnchanged(p, "BS02", new ChooseEventRequest("BACKEND"));
        var result = choose(p, "BS02", multi("BACKEND", "WEB"));
        assertThat(result.player().beforeSchoolDay()).isEqualTo(3);
        assertThat(result.statDelta()).containsEntry("B", 4).containsEntry("F", 4).containsEntry("P", 4);
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), "BS02")).hasSize(2);
    }
    @Test void legacySingleVisitIsPreservedAndOnlyRemainingVisitReceivesEffects() {
        Player p = player();
        p.getStats().apply(Map.of("P", 3, "A", 1));
        p = players.saveAndFlush(p);
        history.saveAndFlush(CommandLog.activity(p, "BS01", "LAB", "{\"scene\":\"기존 방문\"}"));
        var before = p.view();
        var legacy = history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), "BS01").getFirst();
        var options = service.getEvent(p.getId(), "BS01").choices();
        assertThat(options).anySatisfy(c -> { assertThat(c.code()).isEqualTo("LAB"); assertThat(c.enabled()).isFalse(); });
        final Player current = p;
        assertThatThrownBy(() -> choose(current, "BS01", multi("LAB"))).isInstanceOf(BusinessException.class);
        assertThat(players.findById(p.getId()).orElseThrow().view()).isEqualTo(before);
        var result = choose(p, "BS01", multi("CLASSROOM"));
        assertThat(result.player().beforeSchoolDay()).isEqualTo(2);
        assertThat(result.statDelta()).containsExactlyInAnyOrderEntriesOf(Map.of("A", 3, "C", 1));
        assertThat(result.player().stats().get("P")).isEqualTo(before.stats().get("P"));
        var logs = history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), "BS01");
        assertThat(logs).hasSize(2);
        assertThat(logs.getFirst().getId()).isEqualTo(legacy.getId());
        assertThat(logs.getFirst().getEvidenceJson()).isEqualTo(legacy.getEvidenceJson());
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), "BS01"))
                .extracting(ActivityRecord::getChoiceCode).containsExactly("CLASSROOM");
    }
    @Test void legacySingleChoiceBodyCanCompleteRemainingVisit() {
        Player p = player();
        history.saveAndFlush(CommandLog.activity(p, "BS01", "LAB", "{}"));
        assertThat(choose(p, "BS01", new ChooseEventRequest("CLASSROOM")).player().beforeSchoolDay()).isEqualTo(2);
    }
    @Test void allFiveDaysCreateSevenHistoriesAndHandOffToCareerTeam() {
        Player p = player();
        String[] titles = {"학교 탐색", "전공 체험", "기초 프로그래밍", "팀 활동", "진로 상담"};
        ChooseEventRequest[] selections = {multi("LAB", "PROJECT_ROOM"), multi("BACKEND", "WEB"),
                new ChooseEventRequest("FUNCTIONS"), new ChooseEventRequest("DEVELOP"), new ChooseEventRequest("SERVER")};
        var order = new ArrayList<String>();
        int historyCount = 0;
        for (int i = 0; i < 5; i++) {
            String event = "BS%02d".formatted(i + 1);
            var available = service.getEvents(p.getId());
            assertThat(available).hasSize(1);
            assertThat(available.getFirst().title()).isEqualTo(titles[i]);
            order.add(available.getFirst().code());
            if (i >= 2) {
                assertThat(available.getFirst().choices()).hasSize(6);
                invalidAndUnchanged(p, event, multi(selections[i].choiceCode()));
            }
            var result = choose(p, event, selections[i]);
            historyCount += history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), event).size();
            p = players.findById(p.getId()).orElseThrow();
            assertThat(result.player().version()).isEqualTo(p.getVersion());
            if (i == 4) {
                assertThat(result.details().get("nextStep")).isEqualTo("CAREER_PREFERENCE");
                assertThat(result.player().phase()).isEqualTo("CAREER_PICK");
                assertThat(result.player().beforeSchoolDay()).isNull();
            }
        }
        assertThat(order).containsExactly("BS01", "BS02", "BS03", "BS04", "BS05");
        assertThat(historyCount).isEqualTo(7);
        assertThat(records.findByPlayerIdOrderByIdAsc(p.getId())).hasSize(7);
        assertThat(service.getEvents(p.getId())).isEmpty();
        assertThat(service.getEvent(p.getId(), "BS05").choices()).allMatch(c -> !c.enabled());
        final Player finished = p;
        assertThatThrownBy(() -> choose(finished, "BS05", new ChooseEventRequest("SERVER")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WRONG_PHASE));
    }
    @Test void replayDoesNotRepeatEffectsAndNewCommandsRequireCurrentVersionAndDay() {
        Player p = player();
        var request = multi("LAB", "PROJECT_ROOM");
        invalidAndUnchanged(p, "BS02", multi("BACKEND", "WEB"));
        var first = service.chooseEvent(p.getId(), "BS01", p.getVersion(), "retry-bs", request);
        var replay = service.chooseEvent(p.getId(), "BS01", p.getVersion(), "retry-bs", request);
        assertThat(replay.player()).isEqualTo(first.player());
        assertThat(replay.statDelta()).isEqualTo(first.statDelta());
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), "BS01")).hasSize(2);
        assertThatThrownBy(() -> choose(p, "BS02", multi("BACKEND", "WEB")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        assertThatThrownBy(() -> service.chooseEvent(p.getId(), "BS01", p.getVersion(), "retry-bs", multi("LAB", "CLASSROOM")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
    }
}
