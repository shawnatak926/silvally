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
}
