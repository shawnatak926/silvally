package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class SchoolInterviewServiceTest {
    @Autowired ActivityService events;
    @Autowired PlayerRepository players;
    @Autowired InterviewService interviews;
    @Autowired ActivityRecordRepository records;
    @Autowired CommandLogRepository history;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    final String strongAnswer = "지원 동기 관심 활동 프로젝트 경험 문제 해결 오류 입학 계획 학습. ".repeat(8);
    Player candidate(int score) {
        Player p = Player.admission("지원자", "OPEN");
        p.selectAdmissionTrack("GENERAL"); p.passDocuments(); p.prepare("AD03", score); p.prepare("AD04", score);
        return players.saveAndFlush(p);
    }
    ChooseEventRequest answer(Player p, String text) {
        return new ChooseEventRequest("ANSWER", null, interviews.getSchoolQuestion(p.getId()).questionId(), text);
    }
    @Test void freeAnswerIsScoredPersistedAndReplayedAfterFinalDecision() {
        Player p = candidate(95);
        var request = answer(p, strongAnswer);
        String key = UUID.randomUUID().toString();
        var first = events.chooseEvent(p.getId(), "AD05", p.getVersion(), key, request);
        assertThat(first.player().phase()).isEqualTo("BEFORE_SCHOOL");
        assertThat(first.details()).containsKeys("interview", "submitted", "decision", "scene");
        var evaluation = (SchoolInterviewEvaluator.Evaluation) first.details().get("interview");
        assertThat(evaluation.score()).isEqualTo(100);
        assertThat(evaluation.components()).hasSize(4);
        var saved = records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), "AD05");
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getEvidenceJson()).contains(request.questionId().toString(), "problemSolving", "answer");
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(p.getId(), "AD05")).hasSize(1);
        var replay = events.chooseEvent(p.getId(), "AD05", p.getVersion(), key, request);
        assertThat(replay.player()).isEqualTo(first.player());
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), "AD05")).hasSize(1);
    }
    @Test void invalidQuestionOrAnswerDoesNotChangeRunAndValidPoorAnswerFails() {
        Player p = candidate(40);
        var q = interviews.getSchoolQuestion(p.getId());
        var before = p.view();
        for (ChooseEventRequest request : new ChooseEventRequest[]{
                new ChooseEventRequest("ANSWER", null, UUID.randomUUID(), strongAnswer),
                new ChooseEventRequest("ANSWER", null, q.questionId(), " "),
                new ChooseEventRequest("ANSWER", null, q.questionId(), "x".repeat(1201)),
                new ChooseEventRequest("PREPARED")}) {
            assertThatThrownBy(() -> events.chooseEvent(p.getId(), "AD05", p.getVersion(), UUID.randomUUID().toString(), request))
                    .isInstanceOf(BusinessException.class);
            assertThat(players.findById(p.getId()).orElseThrow().view()).isEqualTo(before);
        }
        var result = events.chooseEvent(p.getId(), "AD05", p.getVersion(), UUID.randomUUID().toString(), answer(p, "모르겠습니다"));
        assertThat(result.resultCode()).isEqualTo("FAILED");
        assertThat(result.player().phase()).isEqualTo("ENDED");
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(p.getId(), "AD05")).hasSize(1);
    }
    @Test void ad04PreparesQuestionWithoutAnExtraVersionIncrement() {
        Player p = Player.admission("지원자", "OPEN");
        p.selectAdmissionTrack("GENERAL"); p.passDocuments(); p.prepare("AD03", 95);
        p = players.saveAndFlush(p);
        var result = events.chooseEvent(p.getId(), "AD04", p.getVersion(), UUID.randomUUID().toString(), new ChooseEventRequest("PREPARED"));
        assertThat(result.player().version()).isEqualTo(p.getVersion() + 1);
        assertThat(result.details()).containsKey("interviewQuestion");
        assertThat(interviews.getSchoolQuestion(p.getId())).isEqualTo(result.details().get("interviewQuestion"));
    }
    @Test void httpQuestionAndAnswerContractUsesExistingChoiceApi() throws Exception {
        Player p = candidate(95);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/players/{id}/events/AD05/interview", p.getId()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.kind").value("SCHOOL"));
        var q = interviews.getSchoolQuestion(p.getId());
        String body = tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(answer(p, strongAnswer));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/players/{id}/events/AD05/choices", p.getId())
            .header("X-Expected-Version", p.getVersion()).header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.player.phase").value("BEFORE_SCHOOL"));
    }

    @Test void finalInterviewOffersFreeAnswerInsteadOfPresetScore() {
        Player p = Player.admission("지원자", "OPEN");
        p.selectAdmissionTrack("GENERAL"); p.passDocuments(); p.prepare("AD03", 95); p.prepare("AD04", 95);
        p = players.saveAndFlush(p);
        assertThat(events.getEvent(p.getId(), "AD05").choices()).extracting(c -> c.code()).containsExactly("ANSWER");
    }
}
