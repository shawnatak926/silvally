package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(SchoolFixture.class)
class SchoolEventServiceTest {
    @Autowired PlayerService players;
    @Autowired ActivityService events;
    @Autowired SchoolFixture fixture;
    @Autowired ActivityRecordRepository records;
    @Autowired CommandLogRepository history;
    @Test void schoolEventIsOfferedAndSelectionProducesSceneAndHistoryOnce() {
        UUID id = fixture.enrolledStudent();
        assertThat(events.getEvents(id)).extracting(e -> e.code()).contains("S01");
        long version = fixture.version(id);
        String key = UUID.randomUUID().toString();
        int p = fixture.milli(id, "P");
        var result = events.chooseEvent(id, "S01", version, key, new ChooseEventRequest("COOPERATE"));
        assertThat(result.spentAp()).isEqualTo(1);
        assertThat(result.player().remainingAp()).isEqualTo(2);
        assertThat(result.player().version()).isEqualTo(version + 1);
        assertThat(result.statDelta()).containsEntry("C", 3).doesNotContainKey("P");
        assertThat(fixture.milli(id, "P") - p).isEqualTo(500);
        assertThat(result.details()).containsKeys("scene", "tags");
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "S01")).hasSize(1);
        var logs = history.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "S01");
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getChoiceCode()).isEqualTo("COOPERATE");
        assertThat(logs.getFirst().getEvidenceJson()).contains("scene", "teamwork");
        var replay = events.chooseEvent(id, "S01", version, key, new ChooseEventRequest("COOPERATE"));
        assertThat(replay.player()).isEqualTo(result.player());
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "S01")).hasSize(1);
        assertThat(players.getHistory(id, 0, 50).items()).filteredOn(item -> item.subjectCode().equals("S01"))
                .singleElement().satisfies(item -> {
                    assertThat(item.resultCode()).isEqualTo("COMPLETED");
                    assertThat(item.narrativeStatus()).isEqualTo("SAVED");
                    assertThat(item.narrative()).isNotBlank();
                });
        assertThat(events.getEvents(id)).extracting(e -> e.code()).doesNotContain("S01");
        assertThat(events.getEvent(id, "S01").choices()).allMatch(c -> !c.enabled());
        assertThatThrownBy(() -> events.chooseEvent(id, "S01", fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest("COOPERATE")))
                .isInstanceOf(BusinessException.class);
    }
    @Test void invalidOrUnavailableSelectionsLeaveStatsApAndHistoryUnchanged() {
        UUID id = fixture.enrolledStudent();
        var before = fixture.load(id).view();
        for (String code : new String[]{"UNKNOWN", ""}) {
            assertThatThrownBy(() -> events.chooseEvent(id, "S01", before.version(), UUID.randomUUID().toString(), new ChooseEventRequest(code)))
                    .isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> events.chooseEvent(id, "S02", before.version(), UUID.randomUUID().toString(), new ChooseEventRequest("PRACTICE")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> events.chooseEvent(id, "S01", before.version(), UUID.randomUUID().toString(), new ChooseEventRequest(null, List.of("COOPERATE", "REST"))))
                .isInstanceOf(BusinessException.class);
        assertThat(fixture.load(id).view()).isEqualTo(before);
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "S01")).isEmpty();
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "S01")).isEmpty();
    }
    @Test void insufficientApAndPendingYearDecisionDisableEvents() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> p.spendAp(3));
        var before = fixture.load(id).view();
        assertThat(events.getEvent(id, "S01").choices()).filteredOn(c -> c.code().equals("COOPERATE")).allMatch(c -> !c.enabled());
        assertThatThrownBy(() -> events.chooseEvent(id, "S01", before.version(), UUID.randomUUID().toString(), new ChooseEventRequest("COOPERATE")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_AP));
        assertThat(fixture.load(id).view()).isEqualTo(before);
        fixture.modify(id, p -> p.requireChoice("MAJOR"));
        assertThat(events.getEvent(id, "S01").choices()).allMatch(c -> !c.enabled());
    }
}
