package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.entity.Player;
import com.silvally.repository.PlayerRepository;
import com.silvally.support.AdmissionAnswers;
import com.silvally.support.TestStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class SchoolInterviewResultTest {
    @Autowired PlayerRepository players;
    @Autowired InterviewService interviews;
    @Autowired ActivityService events;
    @Autowired MockMvc mvc;
    @Autowired AdmissionResultService results;
    @Autowired com.silvally.repository.ActivityRecordRepository records;
    @Autowired com.silvally.repository.InterviewQuestionRepository questions;
    @Autowired tools.jackson.databind.ObjectMapper json;
    Player candidate() {
        Player p = Player.admission("결과 조회", "OPEN");
        p.selectAdmissionTrack("GENERAL"); p.passDocuments(); p.prepare("AD03", 95); p.prepare("AD04", 95);
        return players.saveAndFlush(p);
    }
    @Test void savedFinalResultCanBeReadAfterAdmissionHasFinished() throws Exception {
        Player p = candidate(); var q = interviews.getSchoolQuestion(p.getId());
        events.chooseEvent(p.getId(), "AD05", p.getVersion(), UUID.randomUUID().toString(),
                new ChooseEventRequest("ANSWER", null, q.questionId(), AdmissionAnswers.STRONG));
        mvc.perform(get("/api/v1/players/{id}/events/AD05/result", p.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.decision.passed").value(true))
                .andExpect(jsonPath("$.data.interview.score").value(100));
    }
    Player prepared(String track, int preparation) {
        Player p = Player.admission("점수 비교", "OPEN");
        org.springframework.test.util.ReflectionTestUtils.setField(p, "seed", "admission-test");
        TestStats.pin(p);
        p.selectAdmissionTrack(track); p.passDocuments(); p.prepare("AD03", preparation); p.prepare("AD04", preparation);
        return players.saveAndFlush(p);
    }
    com.silvally.dto.response.MutationResult submit(Player p, String text) {
        var q = interviews.getSchoolQuestion(p.getId());
        return events.chooseEvent(p.getId(), "AD05", p.getVersion(), UUID.randomUUID().toString(),
                new ChooseEventRequest("ANSWER", null, q.questionId(), text));
    }
    @Test void samePreparationWithDifferentAnswersChangesActualFinalDecisionAndPersistsReasons() {
        Player good = prepared("GENERAL", 95), poor = prepared("GENERAL", 95);
        var goodStats = good.view().stats(); var poorStats = poor.view().stats();
        var passed = submit(good, AdmissionAnswers.STRONG);
        var failed = submit(poor, "모르겠습니다.");
        assertThat(passed.resultCode()).isEqualTo("PASSED"); assertThat(failed.resultCode()).isEqualTo("FAILED");
        var a = results.get(good.getId(), "AD05"); var b = results.get(poor.getId(), "AD05");
        assertThat(a.interview().score()).isEqualTo(100); assertThat(b.interview().score()).isZero();
        assertThat(a.decision().score()).isCloseTo(81.65, org.assertj.core.data.Offset.offset(.000001));
        assertThat(a.decision().score() - b.decision().score()).isCloseTo(25.0, org.assertj.core.data.Offset.offset(.000001));
        assertThat(a.decision().capacity()).isEqualTo(72);
        assertThat(a.componentScores()).containsEntry("scratch", 95.0).containsEntry("python", 95.0).containsEntry("basic", 22.0);
        assertThat(a.reasonCodes()).contains("TRACK_RANK_WITHIN_CAPACITY", "INTERVIEW_WEIGHT_25_PERCENT");
        assertThat(b.reasonCodes()).contains("GENERAL_RANK_EXCEEDS_CAPACITY");
        assertThat(a.nextPhase()).isEqualTo("BEFORE_SCHOOL"); assertThat(a.beforeSchoolDay()).isEqualTo(1);
        assertThat(b.nextPhase()).isEqualTo("ENDED");
        assertThat(passed.statDelta()).isEmpty(); assertThat(passed.player().stats()).isEqualTo(goodStats);
        assertThat(failed.player().stats()).isEqualTo(poorStats);
        for (var result : java.util.List.of(a, b)) {
            var id = result == a ? good.getId() : poor.getId();
            var saved = records.findByPlayerIdAndEventCodeOrderByIdAsc(id, "AD05").getFirst();
            assertThat(json.readValue(json.readTree(saved.getEvidenceJson()).path("admissionResult").toString(),
                    com.silvally.dto.response.AdmissionResultView.class)).isEqualTo(result);
            assertThat(results.get(id, "AD05")).isEqualTo(result);
        }
    }
    @Test void finalSpecialFailureAppliesGeneralRuleToSameSubmittedScoresOnlyOnce() {
        Player p = prepared("MEISTER", 40);
        var q = interviews.getSchoolQuestion(p.getId());
        var request = new ChooseEventRequest("ANSWER", null, q.questionId(), "모르겠습니다.");
        String key = UUID.randomUUID().toString();
        var first = events.chooseEvent(p.getId(), "AD05", p.getVersion(), key, request);
        assertThat(first.resultCode()).isEqualTo("SHIELD_FAILED");
        var saved = results.get(p.getId(), "AD05");
        assertThat(saved.admissionTrack()).isEqualTo("MEISTER"); assertThat(saved.appliedTrack()).isEqualTo("GENERAL");
        assertThat(saved.reasonCodes()).contains("GENERAL_SHIELD_APPLIED", "GENERAL_SHIELD_FAILED");
        assertThat(saved.decision().generalScore()).isEqualTo(saved.decision().score());
        assertThat(saved.decision().generalRank()).isEqualTo(saved.decision().rank());
        assertThat(saved.decision().shieldActivated()).isTrue();
        assertThat(events.chooseEvent(p.getId(), "AD05", p.getVersion(), key, request).player()).isEqualTo(first.player());
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), "AD05")).hasSize(1);
        assertThat(questions.findById(q.questionId()).orElseThrow().isCompleted()).isTrue();
    }
    @Test void documentShieldPassPreservesTrackAndProceedsToSchoolInterviewAndBeforeSchool() {
        Player p = Player.admission("보호막", "OPEN");
        org.springframework.test.util.ReflectionTestUtils.setField(p, "seed", "admission-test");
        p.selectAdmissionTrack("MEISTER"); p = players.saveAndFlush(p);
        var first = events.chooseEvent(p.getId(), "AD02", p.getVersion(), UUID.randomUUID().toString(), new ChooseEventRequest("ACADEMIC"));
        assertThat(first.resultCode()).isEqualTo("SHIELD_PASSED");
        var documents = results.get(p.getId(), "AD02");
        assertThat(documents.decision().capacity()).isEqualTo(144);
        assertThat(documents.reasonCodes()).contains("GENERAL_SHIELD_PASSED");
        assertThat(documents.componentScores()).containsEntry("essay", 100.0).containsEntry("grades", 100.0).containsEntry("record", 0.0);
        for (String event : java.util.List.of("AD03", "AD04")) {
            p = players.findById(p.getId()).orElseThrow();
            events.chooseEvent(p.getId(), event, p.getVersion(), UUID.randomUUID().toString(), new ChooseEventRequest("PREPARED"));
        }
        p = players.findById(p.getId()).orElseThrow(); submit(p, AdmissionAnswers.STRONG);
        assertThat(results.get(p.getId(), "AD05").admissionTrack()).isEqualTo("MEISTER");
        assertThat(results.get(p.getId(), "AD05").nextPhase()).isEqualTo("BEFORE_SCHOOL");
        assertThat(results.get(p.getId(), "AD02")).isEqualTo(documents);
    }
    @Test void resultReadRejectsMissingSubmissionButDoesNotRescoreLegacyResults() throws Exception {
        Player p = prepared("GENERAL", 95);
        mvc.perform(get("/api/v1/players/{id}/events/AD05/result", p.getId())).andExpect(status().isConflict());
        String evidence = "{\"decision\":{\"passed\":false,\"shieldActivated\":false,\"rank\":144,\"limit\":72,\"score\":20,\"generalScore\":20,\"generalRank\":144},"
                + "\"interview\":{\"score\":0,\"components\":{},\"source\":\"RULE_BASED\"}}";
        records.saveAndFlush(new com.silvally.entity.ActivityRecord(p, "AD05", "BASIC", "FAILED", evidence));
        var result = results.get(p.getId(), "AD05");
        assertThat(result.decision().score()).isEqualTo(20);
        assertThat(result.interview().rubricVersion()).isEqualTo("legacy-unversioned");
        assertThat(result.reasonCodes()).contains("LEGACY_RESULT_WITHOUT_RUBRIC_EVIDENCE");
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), "AD05")).hasSize(1);
    }
}
