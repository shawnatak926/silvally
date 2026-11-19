package com.silvally.service;

import com.silvally.entity.*;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.interview.timeout-ms=25")
class InterviewFallbackServiceTest {
    @MockitoBean InterviewQuestionGenerator generator;
    @Autowired com.silvally.repository.ActivityRecordRepository activities;
    @Autowired InterviewService interviews;
    @Autowired PlayerRepository players;
    @Autowired InterviewQuestionRepository questions;
    Player candidate() {
        Player p = Player.admission("fallback", null);
        p.selectAdmissionTrack("GENERAL"); p.passDocuments(); p.prepare("AD03", 90); p.prepare("AD04", 90);
        return players.saveAndFlush(p);
    }
    @Test void generatorFailurePersistsOneFallbackQuestionAndRetryReturnsIt() throws Exception {
        given(generator.generate(any())).willThrow(new IllegalStateException("AI unavailable"));
        Player p = candidate();
        var first = interviews.getSchoolQuestion(p.getId());
        assertThat(first.source()).isEqualTo("FALLBACK");
        assertThat(interviews.getSchoolQuestion(p.getId())).isEqualTo(first);
        assertThat(questions.findByPlayerIdAndKindAndSubjectCode(p.getId(), InterviewKind.SCHOOL, "AD05")).isPresent();
    }
    @Test void slowGeneratorTimesOutAndDoesNotReplaceSavedFallbackLater() throws Exception {
        given(generator.generate(any())).willAnswer(invocation -> { Thread.sleep(5000); return new InterviewQuestionGenerator.Generated("늦은 질문", "AI"); });
        Player p = candidate();
        var first = interviews.getSchoolQuestion(p.getId());
        assertThat(first.source()).isEqualTo("FALLBACK");
        assertThat(first.question()).doesNotContain("늦은 질문");
        assertThat(interviews.getSchoolQuestion(p.getId())).isEqualTo(first);
    }
    @Test void fallbackKeepsSubmittedMotivationAndActivityContext() throws Exception {
        given(generator.generate(any())).willThrow(new IllegalStateException("no AI key"));
        Player p = candidate();
        activities.saveAndFlush(new ActivityRecord(p, "AD02", "BALANCED", "PASSED",
                "{\"motivation\":\"카페 재고 관리의 불편을 해결하고 싶습니다.\",\"scene\":\"재고 관리 프로그램 제작 경험을 제출했습니다.\"}"));
        var q = interviews.getSchoolQuestion(p.getId());
        assertThat(q.source()).isEqualTo("FALLBACK");
        assertThat(q.question()).contains("카페 재고 관리", "프로그램 제작 경험");
        assertThat(interviews.getSchoolQuestion(p.getId())).isEqualTo(q);
    }
}
