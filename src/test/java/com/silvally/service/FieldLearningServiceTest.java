package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(SchoolFixture.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class FieldLearningServiceTest {
    @Autowired SchoolFixture fixture;
    @Autowired ActivityService events;
    @Autowired FieldLearningService learning;
    @Autowired FieldLearningRepository programs;
    @Autowired ActivityRecordRepository records;
    @Autowired CommandLogRepository history;
    @Autowired PlayerService players;
    @Autowired CareerService career;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    UUID thirdYearThirdTurn(int english) {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> {
            ReflectionTestUtils.setField(p, "year", 3); ReflectionTestUtils.setField(p, "turn", 3);
            p.chooseSemesterFocus("C"); p.getStats().apply(Map.of("EN", english - p.getStats().getEn()));
        });
        return id;
    }
    com.silvally.dto.response.MutationResult choose(UUID id, String path) {
        return events.chooseEvent(id, "J01", fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest(path));
    }
    @Test void fieldLearningChoiceIsRequiredAtThirdYearThirdTurn() {
        UUID id = thirdYearThirdTurn(22);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).containsExactly("J01");
        var before = fixture.load(id).view();
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHOICE_REQUIRED));
        assertThat(fixture.load(id).view()).isEqualTo(before);
        assertThat(learning.get(id).status()).isEqualTo("NOT_APPLIED");
    }
    @Test void domesticSelectionPreparationAndCompletionAreDistinctAndRewardsApplyOnce() {
        UUID id = thirdYearThirdTurn(22);
        long beforeVersion = fixture.version(id);
        var selection = choose(id, "DOMESTIC");
        assertThat(selection.resultCode()).isEqualTo("SELECTED");
        assertThat(selection.player().version()).isEqualTo(beforeVersion + 1);
        assertThat(selection.statDelta()).isEmpty();
        assertThat(learning.get(id).selected()).isTrue();
        assertThat(learning.get(id).prepared()).isFalse();
        assertThat(learning.get(id).completed()).isFalse();
        var prepare = fixture.advance(id);
        assertThat(prepare.player().version()).isEqualTo(selection.player().version() + 1);
        assertThat(learning.get(id).status()).isEqualTo("PREPARING");
        assertThat(learning.get(id).preparedTurn()).isEqualTo(3);
        assertThat(learning.get(id).completed()).isFalse();
        String key = UUID.randomUUID().toString(); long version = fixture.version(id);
        var completed = players.advanceTurn(id, version, key);
        assertThat(completed.player().version()).isEqualTo(version + 1);
        assertThat(completed.statDelta()).containsEntry("P", 3).containsEntry("C", 4);
        assertThat(learning.get(id).status()).isEqualTo("COMPLETED");
        assertThat(learning.get(id).completedTurn()).isEqualTo(4);
        assertThat(learning.get(id).prepared()).isTrue();
        assertThat(learning.get(id).completed()).isTrue();
        assertThat(players.advanceTurn(id, version, key)).isEqualTo(completed);
        fixture.advance(id);
        var milestones = records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "J01_COMPLETE");
        assertThat(milestones).hasSize(1);
        assertThat(milestones.getFirst().getEvidenceJson()).contains("\"P\":2", "\"C\":3");
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "J01_COMPLETE")).hasSize(1);
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "J01_PREPARE").getFirst().getResultCode()).isEqualTo("PREPARING");
        assertThat(players.getHistory(id, 0, 50).items()).filteredOn(item -> item.subjectCode().startsWith("J01"))
                .hasSize(3).allSatisfy(item -> {
                    assertThat(item.narrativeStatus()).isEqualTo("SAVED");
                    assertThat(item.narrative()).isNotBlank();
                });
    }
    @Test void overseasEnglish49IsRejectedAndNeverCompletesDespiteLaterEnglishGrowth() {
        UUID id = thirdYearThirdTurn(49);
        var rejected = choose(id, "SILICON_VALLEY");
        assertThat(rejected.resultCode()).isEqualTo("NOT_SELECTED");
        assertThat(rejected.statDelta()).isEmpty();
        var saved = learning.get(id);
        assertThat(saved.englishAtApplication()).isEqualTo(49);
        assertThat(saved.failureReason()).isEqualTo("ENGLISH_BELOW_50");
        for (int n = 0; n < 3; n++) {
            assertThat(learning.get(id)).isEqualTo(saved);
            var result = fixture.advance(id);
            assertThat(result.statDelta()).containsEntry("EN", 2).containsEntry("C", 1);
            assertThat(learning.get(id)).isEqualTo(saved);
        }
        assertThat(fixture.load(id).getStats().getEn()).isEqualTo(55);
        assertThat(saved.selected()).isFalse(); assertThat(saved.prepared()).isFalse(); assertThat(saved.completed()).isFalse();
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "J01_PREPARE")).isEmpty();
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "J01_COMPLETE")).isEmpty();
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "J01_COMPLETE")).isEmpty();
        assertThat(learning.summary(id).milestones()).extracting(m -> m.resultCode()).containsExactly("NOT_SELECTED");
    }
    @Test void overseasEnglish50IsSelectedAndCompletesWithOnlyItsOwnRewards() {
        UUID id = thirdYearThirdTurn(50);
        choose(id, "SILICON_VALLEY");
        assertThat(learning.get(id).status()).isEqualTo("SELECTED");
        assertThat(learning.get(id).failureReason()).isNull();
        fixture.advance(id);
        var result = fixture.advance(id);
        assertThat(result.statDelta()).containsEntry("EN", 6).containsEntry("C", 4).containsEntry("P", 1);
        assertThat(fixture.load(id).getStats().getEn()).isEqualTo(58);
        assertThat(learning.get(id).englishAtApplication()).isEqualTo(50);
        assertThat(learning.get(id).completed()).isTrue();
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "J01_COMPLETE").getFirst().getEvidenceJson())
                .contains("\"EN\":4", "\"C\":3").doesNotContain("\"P\":2");
    }
    @Test void retryReturnsSavedSelectionAndNewKeysCannotChangePath() {
        UUID id = thirdYearThirdTurn(49);
        long version = fixture.version(id); String key = UUID.randomUUID().toString();
        var request = new ChooseEventRequest("SILICON_VALLEY");
        var first = events.chooseEvent(id, "J01", version, key, request);
        var replay = events.chooseEvent(id, "J01", version, key, request);
        assertThat(replay.player()).isEqualTo(first.player());
        assertThat(replay.resultCode()).isEqualTo("NOT_SELECTED");
        assertThatThrownBy(() -> events.chooseEvent(id, "J01", version, key, new ChooseEventRequest("DOMESTIC")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        assertThatThrownBy(() -> choose(id, "DOMESTIC")).isInstanceOf(BusinessException.class);
        assertThat(learning.get(id).path()).isEqualTo("SILICON_VALLEY");
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "J01")).hasSize(1);
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(id, "J01")).hasSize(1);
    }
    @Test void wrongPeriodAndInvalidPathDoNotCreateApplicationOrChangeRun() {
        UUID id = fixture.enrolledStudent();
        var before = fixture.load(id).view();
        assertThatThrownBy(() -> choose(id, "DOMESTIC")).isInstanceOf(BusinessException.class);
        assertThat(fixture.load(id).view()).isEqualTo(before);
        assertThat(programs.findById(id)).isEmpty();
        UUID eligible = thirdYearThirdTurn(50);
        var eligibleBefore = fixture.load(eligible).view();
        assertThatThrownBy(() -> choose(eligible, "UNKNOWN")).isInstanceOf(BusinessException.class);
        assertThat(fixture.load(eligible).view()).isEqualTo(eligibleBefore);
        assertThat(programs.findById(eligible)).isEmpty();
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(eligible, "J01")).isEmpty();
    }
    @Test void summaryCanBeConsumedByGraduationAndRecordsAreDeletedWithRun() throws Exception {
        UUID id = thirdYearThirdTurn(22);
        choose(id, "DOMESTIC"); fixture.advance(id); fixture.advance(id);
        var summary = learning.summary(id);
        assertThat(summary.program().completed()).isTrue();
        assertThat(summary.milestones()).extracting(m -> m.eventCode()).containsExactly("J01", "J01_PREPARE", "J01_COMPLETE");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/players/{id}/field-learning/summary", id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.program.completed").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.milestones.length()").value(3));
        players.delete(id, fixture.version(id));
        assertThat(programs.findById(id)).isEmpty();
        assertThat(records.findByPlayerIdOrderByIdAsc(id)).isEmpty();
        assertThatThrownBy(() -> learning.get(id)).isInstanceOf(BusinessException.class);
    }
    @Test void englishStartsAt22AndGrowthAndRewardsStayWithinBounds() {
        UUID id = fixture.enrolledStudent();
        assertThat(fixture.load(id).view().stats()).containsEntry("EN", 22);
        fixture.modify(id, p -> p.getStats().apply(Map.of("EN", 1000)));
        fixture.advance(id);
        assertThat(fixture.load(id).getStats().getEn()).isEqualTo(100);
        fixture.modify(id, p -> p.getStats().apply(Map.of("EN", -1000)));
        assertThat(fixture.load(id).getStats().getEn()).isZero();
    }
    @Test void newMainGraduationSnapshotIncludesCompletedAndRejectedFieldLearning() {
        for (int scenario = 0; scenario < 3; scenario++) {
            UUID id = thirdYearThirdTurn(scenario == 1 ? 50 : scenario == 2 ? 49 : 22);
            choose(id, scenario == 0 ? "DOMESTIC" : "SILICON_VALLEY");
            fixture.advance(id); fixture.advance(id);
            fixture.modify(id, Player::readyToGraduate);
            var ending = career.graduate(id, fixture.version(id), UUID.randomUUID().toString(),
                    new com.silvally.dto.request.FinalizeGraduationRequest("UNEMPLOYED", null)).ending();
            var trips = (java.util.List<?>) ending.summary().get("fieldTrips");
            assertThat(trips).hasSize(scenario == 2 ? 1 : 3);
            String saved = tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(trips);
            assertThat(saved).contains(scenario == 2 ? "NOT_SELECTED" : "COMPLETED");
            if (scenario == 2) assertThat(saved).doesNotContain("COMPLETED");
            assertThat(career.getEnding(id).summary().get("fieldTrips")).isEqualTo(trips);
        }
    }
}
