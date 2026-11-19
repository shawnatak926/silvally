package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.response.EndingMutation;
import com.silvally.dto.response.EndingView;
import com.silvally.entity.ActivityRecord;
import com.silvally.entity.ApplicationStatus;
import com.silvally.entity.CourseEnrollment;
import com.silvally.entity.JobApplication;
import com.silvally.entity.Phase;
import com.silvally.entity.PlayerProject;
import com.silvally.entity.ProjectStatus;
import com.silvally.repository.ActivityRecordRepository;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.JobApplicationRepository;
import com.silvally.repository.PlayerProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GraduationEndingTest {

    private static final String BACKEND = "BACKEND";
    private static final String WEB = "WEB";

    @Autowired
    private CareerService careerService;

    @Autowired
    private SchoolFixture fixture;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private PlayerProjectRepository projectRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Test
    @DisplayName("희망 직무와 같은 오퍼를 수락하면 취업 엔딩이 되고 나머지 오퍼는 거절된다")
    void graduate_acceptMatchingOffer_employed() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        JobApplication matching = offer(id, "COMPANY_A", BACKEND, ApplicationStatus.OFFERED);
        JobApplication other = offer(id, "COMPANY_B", WEB, ApplicationStatus.OFFERED);

        EndingMutation result = graduate(id, "EMPLOYMENT", matching.getId());

        assertThat(result.ending().endingCode()).isEqualTo("EMPLOYED");
        assertThat(result.ending().title()).isEqualTo("희망 직무 취업");
        assertThat(result.ending().selectedPath()).isEqualTo("EMPLOYMENT");
        assertThat(result.ending().acceptedCompany()).isEqualTo("COMPANY_A");
        assertThat(result.ending().acceptedRole()).isEqualTo(BACKEND);
        assertThat(result.player().phase()).isEqualTo(Phase.ENDED.name());
        assertThat(statusOf(matching)).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(statusOf(other)).isEqualTo(ApplicationStatus.DECLINED);
        assertThat(careerService.getEnding(id)).isEqualTo(result.ending());
    }

    @Test
    @DisplayName("희망 직무와 다른 오퍼를 수락하면 적성 불일치 취업 엔딩이 된다")
    void graduate_acceptMismatchedOffer_employedMismatch() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        JobApplication mismatched = offer(id, "COMPANY_B", WEB, ApplicationStatus.OFFERED);

        EndingMutation result = graduate(id, "EMPLOYMENT", mismatched.getId());

        assertThat(result.ending().endingCode()).isEqualTo("EMPLOYED_MISMATCH");
        assertThat(result.ending().summary()).containsEntry("aptitudeMismatch", true);
        assertThat(statusOf(mismatched)).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("오퍼가 하나뿐이면 applicationId 없이도 취업을 확정할 수 있다")
    void graduate_employmentWithSingleOffer_withoutApplicationId() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        JobApplication only = offer(id, "COMPANY_A", BACKEND, ApplicationStatus.OFFERED);

        EndingMutation result = graduate(id, "EMPLOYMENT", null);

        assertThat(result.ending().endingCode()).isEqualTo("EMPLOYED");
        assertThat(statusOf(only)).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("오퍼가 있어도 미취업을 명시하면 미취업 엔딩이 되고 오퍼는 모두 거절된다")
    void graduate_explicitUnemployed_declinesOffers() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        JobApplication offered = offer(id, "COMPANY_A", BACKEND, ApplicationStatus.OFFERED);

        EndingMutation result = graduate(id, "UNEMPLOYED", null);

        assertThat(result.ending().endingCode()).isEqualTo("UNEMPLOYED");
        assertThat(result.ending().acceptedCompany()).isNull();
        assertThat(result.ending().summary()).doesNotContainKey("offer");
        assertThat(statusOf(offered)).isEqualTo(ApplicationStatus.DECLINED);
    }

    @Test
    @DisplayName("AUTO는 오퍼가 있으면 FINAL_PATH_REQUIRED로 거부하고 상태를 바꾸지 않는다")
    void graduate_autoWithOffer_finalPathRequired() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        JobApplication offered = offer(id, "COMPANY_A", BACKEND, ApplicationStatus.OFFERED);
        long versionBefore = fixture.version(id);

        assertThatThrownBy(() -> graduate(id, "AUTO", null))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FINAL_PATH_REQUIRED);
                    assertThat(e.getDetails()).containsKey("offers");
                });

        assertThat(statusOf(offered)).isEqualTo(ApplicationStatus.OFFERED);
        assertThat(fixture.load(id).getPhase()).isEqualTo(Phase.GRAD_READY);
        assertThat(fixture.version(id)).isEqualTo(versionBefore);
    }

    @Test
    @DisplayName("AUTO에 applicationId를 함께 보내면 충돌로 거부한다")
    void graduate_autoWithApplicationId_validationError() {
        UUID id = graduatingStudent(BACKEND, BACKEND);

        assertError(() -> graduate(id, "AUTO", UUID.randomUUID()), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("오퍼가 없는 AUTO는 포트폴리오, 학업, 미취업 순으로 진로를 정한다")
    void graduate_autoWithoutOffer_priority() {
        UUID unemployed = graduatingStudent(BACKEND, BACKEND);
        assertThat(graduate(unemployed, "AUTO", null).ending().endingCode()).isEqualTo("UNEMPLOYED");

        UUID academic = graduatingStudent(BACKEND, BACKEND);
        fixture.addStats(academic, Map.of("A", GraduationPolicy.UNIVERSITY_MIN_ACADEMIC));
        EndingMutation university = graduate(academic, "AUTO", null);
        assertThat(university.ending().endingCode()).isEqualTo("UNIVERSITY");
        assertThat(university.ending().selectedPath()).isEqualTo("UNIVERSITY");
        assertThat(university.ending().summary()).containsEntry("requestedPath", "AUTO");

        UUID developer = graduatingStudent(BACKEND, BACKEND);
        fixture.addStats(developer, Map.of("A", GraduationPolicy.UNIVERSITY_MIN_ACADEMIC));
        project(developer, "PORTFOLIO_APP", ProjectStatus.COMPLETE, true);
        assertThat(graduate(developer, "AUTO", null).ending().endingCode()).isEqualTo("INDIE_DEV");
    }

    @Test
    @DisplayName("포트폴리오 작품이 없으면 개인 개발을 명시해도 LOCKED이다")
    void graduate_indieDevWithoutPortfolio_locked() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        project(id, "UNFINISHED_APP", ProjectStatus.FAILED, false);

        assertError(() -> graduate(id, "INDIE_DEV", null), ErrorCode.LOCKED);
        assertThat(fixture.load(id).getPhase()).isEqualTo(Phase.GRAD_READY);
    }

    @Test
    @DisplayName("거절·불합격한 지원 내역은 수락할 수 없다")
    void graduate_employmentWithDeclinedApplication_locked() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        JobApplication declined = offer(id, "COMPANY_A", BACKEND, ApplicationStatus.DECLINED);
        JobApplication rejected = offer(id, "COMPANY_B", BACKEND, ApplicationStatus.REJECTED);

        assertThatThrownBy(() -> graduate(id, "EMPLOYMENT", declined.getId()))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOCKED);
                    assertThat(e.getDetails()).containsEntry("status", "DECLINED");
                });
        assertError(() -> graduate(id, "EMPLOYMENT", rejected.getId()), ErrorCode.LOCKED);
        assertError(() -> graduate(id, "EMPLOYMENT", null), ErrorCode.LOCKED);
    }

    @Test
    @DisplayName("오퍼가 여러 개인데 applicationId가 없으면 취업을 확정할 수 없다")
    void graduate_employmentWithMultipleOffers_requiresApplicationId() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        offer(id, "COMPANY_A", BACKEND, ApplicationStatus.OFFERED);
        offer(id, "COMPANY_B", WEB, ApplicationStatus.OFFERED);

        assertError(() -> graduate(id, "EMPLOYMENT", null), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("다른 회차의 지원 내역이나 알 수 없는 진로 코드는 거부한다")
    void graduate_unknownApplicationOrPath_rejected() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        UUID otherPlayer = graduatingStudent(BACKEND, BACKEND);
        JobApplication foreign = offer(otherPlayer, "COMPANY_A", BACKEND, ApplicationStatus.OFFERED);

        assertError(() -> graduate(id, "EMPLOYMENT", foreign.getId()), ErrorCode.NOT_FOUND);
        assertError(() -> graduate(id, "ASTRONAUT", null), ErrorCode.VALIDATION_ERROR);
        assertError(() -> graduate(id, "UNIVERSITY", foreign.getId()), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("졸업 요약에 최초·현재 희망 직무, 자격증, 프로젝트, 현장학습 결과가 남는다")
    @SuppressWarnings("unchecked")
    void graduate_summaryKeepsCareerCertificatesProjectsAndFieldTrips() {
        UUID id = graduatingStudent("WEB", BACKEND);
        passCourse(id, "INFO_CRAFTSMAN");
        project(id, "PORTFOLIO_APP", ProjectStatus.COMPLETE, true);
        project(id, "UNFINISHED_APP", ProjectStatus.FAILED, false);
        fieldTrip(id, "FIELD_TRIP_SILICON_VALLEY", "APPLY", "NOT_SELECTED");
        fieldTrip(id, "FIELD_PRACTICE_DOMESTIC", "APPLY", "PARTICIPATED");
        activityRecordRepository.save(activity(id, "BS01", "LAB", "DONE"));

        EndingView ending = graduate(id, "UNIVERSITY", null).ending();

        Map<String, Object> summary = ending.summary();
        assertThat(summary).containsEntry("initialCareerCode", "WEB")
                .containsEntry("currentCareerCode", BACKEND)
                .containsEntry("careerChanged", true)
                .containsEntry("portfolioCount", 1);
        assertThat((List<Map<String, Object>>) summary.get("certificates")).singleElement()
                .satisfies(c -> {
                    assertThat(c).containsEntry("courseCode", "INFO_CRAFTSMAN");
                    assertThat(c).containsEntry("certificate", "정보처리기능사");
                });
        assertThat((List<Map<String, Object>>) summary.get("projects"))
                .extracting(p -> p.get("projectCode"), p -> p.get("status"), p -> p.get("inPortfolio"))
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("PORTFOLIO_APP", "COMPLETE", true),
                        org.assertj.core.groups.Tuple.tuple("UNFINISHED_APP", "FAILED", false));
        assertThat((List<Map<String, Object>>) summary.get("fieldTrips"))
                .extracting(t -> t.get("eventCode"), t -> t.get("resultCode"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("FIELD_TRIP_SILICON_VALLEY", "NOT_SELECTED"),
                        org.assertj.core.groups.Tuple.tuple("FIELD_PRACTICE_DOMESTIC", "PARTICIPATED"));
    }

    @Test
    @DisplayName("졸업 전에는 엔딩을 조회할 수 없고, 졸업 단계가 아니면 졸업할 수 없다")
    void ending_beforeGraduation_notReady() {
        UUID student = fixture.enrolledStudent();

        assertError(() -> careerService.getEnding(student), ErrorCode.ENDING_NOT_READY);
        assertError(() -> graduate(student, "UNEMPLOYED", null), ErrorCode.WRONG_PHASE);

        UUID ready = graduatingStudent(BACKEND, BACKEND);
        assertError(() -> careerService.getEnding(ready), ErrorCode.ENDING_NOT_READY);
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 재시도하면 같은 엔딩을 돌려주고 한 번만 반영한다")
    void graduate_sameKeyRetry_replaysResponse() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        JobApplication offered = offer(id, "COMPANY_A", BACKEND, ApplicationStatus.OFFERED);
        long version = fixture.version(id);
        String key = UUID.randomUUID().toString();
        FinalizeGraduationRequest request = new FinalizeGraduationRequest("EMPLOYMENT", offered.getId());

        EndingMutation first = careerService.graduate(id, version, key, request);
        long versionAfter = fixture.version(id);
        EndingMutation retry = careerService.graduate(id, version, key, request);

        assertThat(retry).isEqualTo(first);
        assertThat(fixture.version(id)).isEqualTo(versionAfter);
        assertThat(statusOf(offered)).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("이미 졸업한 회차는 다시 졸업할 수 없다")
    void graduate_alreadyEnded_wrongPhase() {
        UUID id = graduatingStudent(BACKEND, BACKEND);
        graduate(id, "UNEMPLOYED", null);

        assertError(() -> graduate(id, "UNEMPLOYED", null), ErrorCode.WRONG_PHASE);
    }

    private UUID graduatingStudent(String initialCareer, String currentCareer) {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> {
            player.readyToGraduate();
            ReflectionTestUtils.setField(player, "initialCareerCode", initialCareer);
            ReflectionTestUtils.setField(player, "currentCareerCode", currentCareer);
        });
        return id;
    }

    private EndingMutation graduate(UUID id, String path, UUID applicationId) {
        return careerService.graduate(id, fixture.version(id), UUID.randomUUID().toString(),
                new FinalizeGraduationRequest(path, applicationId));
    }

    private JobApplication offer(UUID playerId, String company, String role, ApplicationStatus status) {
        return applicationRepository.save(newEntity(JobApplication.class, Map.of(
                "playerId", playerId, "companyCode", company, "role", role,
                "status", status, "appliedTurn", 1)));
    }

    private PlayerProject project(UUID playerId, String code, ProjectStatus status, boolean inPortfolio) {
        return projectRepository.save(newEntity(PlayerProject.class, Map.of(
                "playerId", playerId, "projectCode", code, "year", 3, "startTurn", 1, "endTurn", 5,
                "status", status, "quality", 80, "inPortfolio", inPortfolio)));
    }

    private void passCourse(UUID playerId, String courseCode) {
        CourseEnrollment enrollment = CourseEnrollment.enroll(playerId, courseCode, 2);
        enrollment.recordExam(new BigDecimal("90"), true, 4);
        enrollmentRepository.save(enrollment);
    }

    private void fieldTrip(UUID playerId, String eventCode, String choiceCode, String resultCode) {
        activityRecordRepository.save(activity(playerId, eventCode, choiceCode, resultCode));
    }

    private ActivityRecord activity(UUID playerId, String eventCode, String choiceCode, String resultCode) {
        return new ActivityRecord(fixture.load(playerId), eventCode, choiceCode, resultCode, "{}");
    }

    private ApplicationStatus statusOf(JobApplication application) {
        return applicationRepository.findById(application.getId()).orElseThrow().getStatus();
    }

    private static <T> T newEntity(Class<T> type, Map<String, Object> fields) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            T entity = constructor.newInstance();
            fields.forEach((name, value) -> ReflectionTestUtils.setField(entity, name, value));
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
