package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.dto.response.InterviewView;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class InterviewServiceTest {
    @Autowired ActivityRecordRepository activities;
    @Autowired InterviewService interviews;
    @Autowired PlayerRepository players;
    @Autowired JobApplicationRepository applications;
    @Autowired InterviewQuestionRepository questions;
    @Autowired PlayerService playerService;

    Player candidate() {
        Player p = Player.admission("김일강", "OPEN");
        p.selectAdmissionTrack("GENERAL"); p.passDocuments(); p.prepare("AD03", 90); p.prepare("AD04", 90);
        return players.saveAndFlush(p);
    }
    @Test void repeatedSchoolQuestionIsPersistedWithoutChangingRunVersion() {
        Player p = candidate();
        InterviewView first = interviews.getSchoolQuestion(p.getId());
        assertThat(interviews.getSchoolQuestion(p.getId())).isEqualTo(first);
        assertThat(first.kind()).isEqualTo("SCHOOL");
        assertThat(first.source()).isEqualTo("MOCK");
        assertThat(first.question()).isNotBlank();
        assertThat(questions.findById(first.questionId())).isPresent();
        assertThat(players.findById(p.getId()).orElseThrow().getVersion()).isEqualTo(p.getVersion());
    }
    @Test void wrongRunQuestionAndInvalidAnswersAreRejectedWithoutConsumingQuestion() {
        Player a = candidate(), b = candidate();
        var qa = interviews.getSchoolQuestion(a.getId());
        var qb = interviews.getSchoolQuestion(b.getId());
        for (String answer : new String[]{"", "  ", "x".repeat(1201)}) {
            assertThatThrownBy(() -> interviews.validateAnswer(a, InterviewKind.SCHOOL, "AD05", qa.questionId(), answer))
                .isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> interviews.validateAnswer(a, InterviewKind.SCHOOL, "AD05", qb.questionId(), "지원 동기"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> interviews.validateAnswer(a, InterviewKind.SCHOOL, "AD05", UUID.randomUUID(), "지원 동기"))
                .isInstanceOf(BusinessException.class);
        assertThat(interviews.validateAnswer(a, InterviewKind.SCHOOL, "AD05", qa.questionId(), "  지원 동기  "))
                .isEqualTo("지원 동기");
        assertThat(interviews.validateAnswer(a, InterviewKind.SCHOOL, "AD05", qa.questionId(), "x".repeat(1200))).hasSize(1200);
    }
    @Test void questionsRequireTheCorrectStage() {
        Player p = players.saveAndFlush(Player.admission("지원자", null));
        assertThatThrownBy(() -> interviews.getSchoolQuestion(p.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> interviews.getSchoolQuestion(UUID.randomUUID())).isInstanceOf(BusinessException.class);
    }
    @Test void companyContextIsIsolatedFromSchoolAndOtherApplications() {
        Player p = candidate();
        var school = interviews.getSchoolQuestion(p.getId());
        p.startSchool(3);
        ReflectionTestUtils.setField(p, "year", 3); ReflectionTestUtils.setField(p, "turn", 5);
        p = players.saveAndFlush(p);
        JobApplication a = application(p.getId(), "A"), b = application(p.getId(), "B");
        var company = interviews.getCompanyQuestion(p.getId(), a.getId());
        assertThat(company.kind()).isEqualTo("COMPANY");
        assertThat(company.question()).isNotEqualTo(school.question());
        Player current = p;
        assertThatThrownBy(() -> interviews.validateAnswer(current, InterviewKind.COMPANY, a.getId().toString(), school.questionId(), "답변"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> interviews.validateAnswer(current, InterviewKind.COMPANY, b.getId().toString(), company.questionId(), "답변"))
                .isInstanceOf(BusinessException.class);
        assertThat(interviews.validateAnswer(p, InterviewKind.COMPANY, a.getId().toString(), company.questionId(), "회사 답변")).isEqualTo("회사 답변");
        assertThat(applications.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.CODE_PASSED);
    }
    @Test void deletingRunRemovesItsQuestions() {
        Player p = candidate();
        var q = interviews.getSchoolQuestion(p.getId());
        playerService.delete(p.getId(), p.getVersion());
        assertThat(questions.findById(q.questionId())).isEmpty();
    }
    @Test void questionReferencesActivitiesAndCompletedInterviewCannotBeSubmittedAgain() {
        Player p = candidate();
        activities.saveAndFlush(new ActivityRecord(p, "AD03", "PREPARED", "COMPLETED", "{\"scene\":\"직접 만든 계산기 프로젝트\"}"));
        var q = interviews.getSchoolQuestion(p.getId());
        assertThat(q.question()).contains("직접 만든 계산기 프로젝트");
        var result = interviews.submitAnswer(p, InterviewKind.SCHOOL, "AD05", q.questionId(), " 지원 동기 ");
        assertThat(result.answer()).isEqualTo("지원 동기");
        assertThat(result.kind()).isEqualTo("SCHOOL");
        assertThatThrownBy(() -> interviews.submitAnswer(p, InterviewKind.SCHOOL, "AD05", q.questionId(), "다른 답변"))
                .isInstanceOf(BusinessException.class);
    }
    JobApplication application(UUID player, String code) {
        JobApplication a = org.springframework.beans.BeanUtils.instantiateClass(JobApplication.class);
        ReflectionTestUtils.setField(a, "playerId", player); ReflectionTestUtils.setField(a, "companyCode", code);
        ReflectionTestUtils.setField(a, "role", "BACKEND"); ReflectionTestUtils.setField(a, "status", ApplicationStatus.CODE_PASSED);
        return applications.saveAndFlush(a);
    }
    @Test void simultaneousQuestionRefreshesShareOneQuestionAndDoNotChangeVersion() throws Exception {
        Player p = candidate();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var futures = new java.util.ArrayList<java.util.concurrent.Future<com.silvally.dto.response.InterviewView>>();
            for (int i = 0; i < 6; i++) futures.add(pool.submit(() -> { start.await(); return interviews.getSchoolQuestion(p.getId()); }));
            start.countDown();
            var first = futures.getFirst().get(10, java.util.concurrent.TimeUnit.SECONDS);
            for (var future : futures) assertThat(future.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(first);
        }
        assertThat(players.findById(p.getId()).orElseThrow().getVersion()).isEqualTo(p.getVersion());
    }
}
