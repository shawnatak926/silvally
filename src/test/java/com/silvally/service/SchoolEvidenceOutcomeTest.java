package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.entity.LifeNarrative;
import com.silvally.entity.NarrativeSource;
import com.silvally.entity.NarrativeStatus;
import com.silvally.entity.SchoolEventDraw;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.FieldLearningRepository;
import com.silvally.repository.JobApplicationRepository;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerProjectRepository;
import com.silvally.repository.SchoolEventDrawRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@Import(SchoolFixture.class)
class SchoolEvidenceOutcomeTest {

    private static final List<String> INTRO_ONLY_SOURCES = List.of(
            "EQUIPMENT_SUPPORT", "GLOBAL_PREPARATION", "EMPLOYMENT_CAMPS", "INDUSTRY_PROJECT", "POST_EMPLOYMENT_STUDY");

    @MockitoSpyBean NarrativeGenerator generator;
    @Autowired SchoolFixture fixture;
    @Autowired ActivityService events;
    @Autowired PlayerService players;
    @Autowired SchoolLifeContent content;
    @Autowired SchoolLifeService life;
    @Autowired NarrativeRecoveryService recovery;
    @Autowired SchoolEventDrawRepository draws;
    @Autowired LifeNarrativeRepository narratives;
    @Autowired FieldLearningRepository fieldLearning;
    @Autowired CourseEnrollmentRepository enrollments;
    @Autowired JobApplicationRepository applications;
    @Autowired PlayerProjectRepository projects;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @AfterEach
    void resetGenerator() {
        reset(generator);
    }

    @Test
    void introPlanAndPracticeEventsCarryEvidenceButCreateNoProgramResults() {
        var activities = content.activities().stream()
                .filter(a -> INTRO_ONLY_SOURCES.contains(a.event().source())).toList();
        assertThat(activities).extracting(a -> a.event().source()).containsAll(INTRO_ONLY_SOURCES);

        for (var activity : activities) {
            UUID id = fixture.enrolledStudent();
            int year = activity.years().getFirst(), turn = activity.minTurn();
            atPeriod(id, year, turn);
            var event = activity.event();
            draws.saveAndFlush(new SchoolEventDraw(id, year + ":" + turn + ":false:0", "DAILY", json.writeValueAsString(event)));
            int resume = fixture.load(id).getResumeLevel();

            events.chooseEvent(id, event.code(), fixture.version(id), UUID.randomUUID().toString(),
                    new ChooseEventRequest(event.choices().getFirst().code()));

            var player = fixture.load(id);
            assertThat(fieldLearning.findById(id)).describedAs(event.code()).isEmpty();
            assertThat(enrollments.findByPlayerId(id)).describedAs(event.code()).isEmpty();
            assertThat(applications.findByPlayerIdOrderByCompanyCodeAsc(id)).describedAs(event.code()).isEmpty();
            assertThat(projects.findByPlayerId(id)).describedAs(event.code()).isEmpty();
            assertThat(player.getResumeLevel()).isEqualTo(resume);
            assertThat(player.getPracticeRemaining()).isZero();
            assertThat(player.getEndingCode()).isNull();

            NarrativeContext context = context(id, event.code());
            assertThat(context.schoolEvidence()).extracting(NarrativeContext.SchoolEvidence::code)
                    .startsWith(event.source());
            assertThat(context.certificates()).isEmpty();
            assertThat(context.projects()).isEmpty();
        }
    }

    @Test
    void scheduledConflictSceneRecordsSourceWithoutFieldLearningSelection() {
        UUID id = fixture.enrolledStudent();
        atPeriod(id, 2, 19);

        new TransactionTemplate(transactions).executeWithoutResult(status ->
                life.recordTurnScene(fixture.load(id), Map.of("total", 0)));

        NarrativeContext context = context(id, "SCHOOL_TIMELINE_Y2_T19");
        assertThat(context.schoolEvidence()).extracting(NarrativeContext.SchoolEvidence::code)
                .containsExactly("GLOBAL_PERIOD_NOTE");
        assertThat(context.schoolEvidence().getFirst().classification()).isEqualTo("CONFLICT");
        assertThat(fieldLearning.findById(id)).isEmpty();
    }

    @Test
    void legacySnapshotWithoutSchoolEvidenceIsRetriedOnceAndKeepsVersionAndSavedText() {
        doThrow(new IllegalStateException("AI timeout")).when(generator).generate(any());
        UUID id = players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("이전 회차", null)).id();
        events.chooseEvent(id, "AD01", fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest("GENERAL"));
        LifeNarrative failed = narrativeOf(id, "AD01");
        assertThat(failed.getStatus()).isEqualTo(NarrativeStatus.FAILED);
        ObjectNode legacy = (ObjectNode) json.readTree(failed.getContextJson());
        legacy.remove("schoolEvidence");
        jdbc.update("update life_narrative set context_json = ? where id = ?", json.writeValueAsString(legacy), failed.getId());
        long version = fixture.version(id);
        reset(generator);

        var first = recovery.retry(id);
        LifeNarrative saved = narrativeOf(id, "AD01");
        var second = recovery.retry(id);

        assertThat(first.saved()).isEqualTo(1);
        assertThat(second.retried()).isZero();
        assertThat(saved.getStatus()).isEqualTo(NarrativeStatus.SAVED);
        assertThat(saved.getSource()).isEqualTo(NarrativeSource.TEMPLATE);
        assertThat(narrativeOf(id, "AD01").getText()).isEqualTo(saved.getText());
        assertThat(json.readValue(saved.getContextJson(), NarrativeContext.class).schoolEvidence()).isNull();
        assertThat(fixture.version(id)).isEqualTo(version);
        assertThat(players.getHistory(id, 0, 20).items().getFirst().narrativeStatus()).isEqualTo("SAVED");
    }

    private void atPeriod(UUID id, int year, int turn) {
        fixture.modify(id, p -> {
            ReflectionTestUtils.setField(p, "year", year);
            ReflectionTestUtils.setField(p, "turn", turn);
            p.chooseSemesterFocus("C");
            p.getStats().apply(Map.of("S", 20 - p.getStats().getS()));
        });
    }

    private NarrativeContext context(UUID playerId, String sceneId) {
        return json.readValue(narrativeOf(playerId, sceneId).getContextJson(), NarrativeContext.class);
    }

    private LifeNarrative narrativeOf(UUID playerId, String sceneId) {
        return narratives.findAll().stream()
                .filter(n -> n.getPlayerId().equals(playerId) && sceneId.equals(n.getSceneId()))
                .findFirst().orElseThrow();
    }
}
