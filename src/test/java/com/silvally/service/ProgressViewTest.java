package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.ProgressView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SchoolFixture.class)
class ProgressViewTest {

    private static final String INFO = "INFO_CRAFTSMAN";
    private static final String API = "REST_API_SERVER";

    @Autowired SchoolFixture fixture;
    @Autowired PlayerService players;
    @Autowired CertificateService certificates;
    @Autowired ProjectService projects;
    @Autowired ObjectMapper json;

    @Test
    void progressCollectsSavedCoursesProjectsSnapshotsAndExamsByYear() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("P", 60, "A", 60, "B", 50));
        certificates.enroll(id, INFO, fixture.version(id), key());
        certificates.attend(id, INFO, fixture.version(id), key());
        projects.start(id, API, fixture.version(id), key());
        projects.work(id, API, fixture.version(id), key());
        fixture.advance(id);
        certificates.attend(id, INFO, fixture.version(id), key());
        certificates.takeExam(id, INFO, fixture.version(id), key());
        projects.work(id, API, fixture.version(id), key());
        fixture.advanceTimes(id, 5);

        ProgressView progress = players.getProgress(id);

        assertThat(progress.player().turn()).isEqualTo(7);
        assertThat(progress.enrollments()).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("courseCode", INFO).containsEntry("status", "PASSED")
                    .containsEntry("attendance", 2).containsEntry("year", 1);
        });
        assertThat(progress.qualifications()).extracting(row -> row.get("courseCode")).containsExactly(INFO);
        assertThat(progress.projects()).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("projectCode", API).containsEntry("status", "COMPLETE");
            assertThat(row.get("quality")).isNotNull();
        });
        assertThat(progress.snapshots()).hasSize(2).allSatisfy(row -> assertThat(row).containsEntry("projectCode", API));
        assertThat(progress.yearSummaries()).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("year", 1);
            assertThat((List<?>) row.get("exams")).singleElement()
                    .satisfies(exam -> assertThat(((Map<?, ?>) exam).get("kind")).isEqualTo("MIDTERM"));
            assertThat(row.get("gradeAfter")).isEqualTo(progress.player().stats().get("G"));
        });
    }

    @Test
    void progressSerializesEnrollmentsFieldAndWorksBeforeSchool() {
        UUID id = players.create(key(), new com.silvally.dto.request.CreatePlayerRequest("입학 전", null)).id();

        ProgressView progress = players.getProgress(id);
        String body = json.writeValueAsString(progress);

        assertThat(progress.enrollments()).isEmpty();
        assertThat(progress.yearSummaries()).isEmpty();
        assertThat(body).contains("\"enrollments\":[]").doesNotContain("enrolments");
    }

    @Test
    void unknownPlayerProgressIsNotFound() {
        assertThatThrownBy(() -> players.getProgress(UUID.randomUUID())).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
