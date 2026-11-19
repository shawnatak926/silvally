package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ApplyJobRequest;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.request.JobStepRequest;
import com.silvally.dto.response.ApplicationMutation;
import com.silvally.dto.response.ApplicationView;
import com.silvally.dto.response.EndingMutation;
import com.silvally.entity.PlayerProject;
import com.silvally.repository.PlayerProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RecruitmentServiceTest {

    private static final String COMPANY = "BYTE_WORKS";

    @Autowired
    private CareerService careerService;

    @Autowired
    private PlayerProjectRepository projectRepository;

    @Autowired
    private SchoolFixture fixture;

    @Test
    @DisplayName("서류·코딩·회사 면접·결과를 차례로 통과하면 오퍼를 받는다")
    void fullPipeline_offered() {
        UUID id = applicant(true);
        strong(id);

        ApplicationMutation applied = apply(id, COMPANY);
        assertThat(applied.application().status()).isEqualTo("DOC_PASSED");
        assertThat(applied.application().p()).isEqualByComparingTo("45");
        assertThat(applied.application().d()).isEqualByComparingTo("32");
        assertThat(applied.change().spentAp()).isEqualTo(1);

        UUID applicationId = applied.application().id();
        assertThat(step(id, applicationId, "CODING").application().status()).isEqualTo("CODE_PASSED");
        ApplicationMutation interview = step(id, applicationId, "INTERVIEW");
        assertThat(interview.application().status()).isEqualTo("INTERVIEW_PASSED");
        assertThat(interview.application().j()).isNotNull();

        ApplicationMutation result = step(id, applicationId, "RESULT");
        assertThat(result.application().status()).isEqualTo("OFFERED");
        assertThat(result.application().failureCode()).isNull();
        assertThat(careerService.getApplications(id)).singleElement()
                .satisfies(view -> assertThat(view.status()).isEqualTo("OFFERED"));
    }

    @Test
    @DisplayName("회사 면접 점수가 기준에 못 미치면 탈락하고 결과 단계로 갈 수 없다")
    void interview_belowPassScore_rejected() {
        UUID id = applicant(true);
        UUID applicationId = apply(id, COMPANY).application().id();
        fixture.addStats(id, Map.of("P", 50, "B", 50));
        step(id, applicationId, "CODING");
        fixture.addStats(id, Map.of("C", -15, "A", -15));

        ApplicationMutation interview = step(id, applicationId, "INTERVIEW");

        assertThat(interview.application().status()).isEqualTo("REJECTED");
        assertThat(interview.application().failureCode()).isEqualTo("INTERVIEW_FAILED");
        assertThat((int) interview.change().details().get("score"))
                .isLessThan((int) interview.change().details().get("passScore"));
        assertError(() -> step(id, applicationId, "RESULT"), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("서류 점수가 기준에 못 미치면 지원 즉시 탈락한다")
    void apply_lowDocumentScore_rejected() {
        UUID id = applicant(false);

        ApplicationMutation applied = apply(id, COMPANY);

        assertThat(applied.application().status()).isEqualTo("REJECTED");
        assertThat(applied.application().failureCode()).isEqualTo("DOC_FAILED");
        assertThat(applied.change().resultCode()).isEqualTo("REJECTED");
        assertError(() -> step(id, applied.application().id(), "CODING"), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("코딩 테스트 점수가 기준에 못 미치면 탈락한다")
    void coding_belowPassScore_rejected() {
        UUID id = applicant(true);
        UUID applicationId = apply(id, COMPANY).application().id();

        ApplicationMutation coding = step(id, applicationId, "CODING");

        assertThat(coding.application().status()).isEqualTo("REJECTED");
        assertThat(coding.application().failureCode()).isEqualTo("CODING_FAILED");
    }

    @Test
    @DisplayName("종합 점수가 기준에 못 미치면 면접까지 통과해도 결과에서 탈락한다")
    void result_belowOfferScore_rejected() {
        UUID id = applicant(true);
        fixture.addStats(id, Map.of("P", 11, "B", 23, "C", 11, "A", 7));
        UUID applicationId = apply(id, COMPANY).application().id();
        step(id, applicationId, "CODING");
        step(id, applicationId, "INTERVIEW");

        ApplicationMutation result = step(id, applicationId, "RESULT");

        assertThat(result.application().status()).isEqualTo("REJECTED");
        assertThat(result.application().failureCode()).isEqualTo("RESULT_FAILED");
    }

    @Test
    @DisplayName("각 단계는 직전 단계를 통과해야만 진행할 수 있다")
    void steps_requirePreviousStepPassed() {
        UUID id = applicant(true);
        strong(id);
        UUID applicationId = apply(id, COMPANY).application().id();

        assertThatThrownBy(() -> step(id, applicationId, "INTERVIEW"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WRONG_STEP);
                    assertThat(e.getDetails()).containsEntry("required", "CODE_PASSED");
                });
        assertError(() -> step(id, applicationId, "RESULT"), ErrorCode.WRONG_STEP);

        step(id, applicationId, "CODING");
        assertError(() -> step(id, applicationId, "CODING"), ErrorCode.WRONG_STEP);
        assertError(() -> step(id, applicationId, "RESULT"), ErrorCode.WRONG_STEP);
        assertError(() -> step(id, applicationId, "PRESENTATION"), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("중복 지원, 학년 미달, 없는 기업, 남의 지원 내역은 거부한다")
    void apply_invalidRequests_rejected() {
        UUID id = applicant(true);
        UUID applicationId = apply(id, COMPANY).application().id();

        assertError(() -> apply(id, COMPANY), ErrorCode.LOCKED);
        assertError(() -> apply(id, "NO_SUCH_COMPANY"), ErrorCode.NOT_FOUND);

        UUID freshman = fixture.enrolledStudent();
        assertError(() -> apply(freshman, COMPANY), ErrorCode.LOCKED);
        assertError(() -> step(freshman, applicationId, "CODING"), ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("마감이 지난 기업에는 지원할 수 없다")
    void apply_afterDeadline_locked() {
        UUID id = applicant(true);
        fixture.modify(id, player -> {
            for (int i = 0; i < 10; i++) {
                player.nextTurn(SchoolRules.AP_PER_TURN);
            }
        });

        assertThatThrownBy(() -> apply(id, COMPANY))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOCKED);
                    assertThat(e.getDetails().get("reasons").toString()).contains("마감");
                });
    }

    @Test
    @DisplayName("마감이 지난 진행 중 지원은 턴 종료 때 만료되고 오퍼는 유지된다")
    void advanceTurn_afterDeadline_expiresOnlyInProgress() {
        UUID id = applicant(true);
        registeredProject(id, "WEB_PORTFOLIO_SITE", 90);
        strong(id);
        UUID offered = apply(id, COMPANY).application().id();
        step(id, offered, "CODING");
        step(id, offered, "INTERVIEW");
        step(id, offered, "RESULT");
        fixture.advance(id);
        UUID inProgress = apply(id, "PIXEL_LAB").application().id();

        // 마감(58턴) 직전인 3학년 10턴(누적 58)에서 턴을 끝내면 다음 턴(59)이 마감을 넘는다.
        fixture.modify(id, player -> {
            for (int i = 0; i < 8; i++) {
                player.nextTurn(SchoolRules.AP_PER_TURN);
            }
        });

        var last = fixture.advance(id);

        assertThat(last.details()).containsEntry("expiredApplications", List.of("PIXEL_LAB"));
        assertThat(statusOf(id, inProgress)).isEqualTo("EXPIRED");
        assertThat(statusOf(id, offered)).isEqualTo("OFFERED");
        assertError(() -> step(id, inProgress, "CODING"), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 재시도하면 지원을 한 번만 반영한다")
    void apply_sameKeyRetry_appliesOnce() {
        UUID id = applicant(true);
        long version = fixture.version(id);
        String key = UUID.randomUUID().toString();
        ApplyJobRequest request = new ApplyJobRequest(COMPANY);

        ApplicationMutation first = careerService.apply(id, version, key, request);
        ApplicationMutation retry = careerService.apply(id, version, key, request);

        assertThat(retry).isEqualTo(first);
        assertThat(careerService.getApplications(id)).hasSize(1);
    }

    @Test
    @DisplayName("채용 오퍼는 졸업 때 수락해 취업 엔딩으로 이어진다")
    void offer_acceptedAtGraduation() {
        UUID id = applicant(true);
        strong(id);
        UUID applicationId = apply(id, COMPANY).application().id();
        step(id, applicationId, "CODING");
        step(id, applicationId, "INTERVIEW");
        step(id, applicationId, "RESULT");
        fixture.modify(id, player -> player.readyToGraduate());

        EndingMutation ending = careerService.graduate(id, fixture.version(id), UUID.randomUUID().toString(),
                new FinalizeGraduationRequest("EMPLOYMENT", applicationId));

        assertThat(ending.ending().acceptedCompany()).isEqualTo(COMPANY);
        assertThat(statusOf(id, applicationId)).isEqualTo("ACCEPTED");
    }

    /** 3학년 1턴(누적 49턴, 1학년 24턴 기준) 학생. 포트폴리오가 있으면 백엔드 작품 1개(완성도 90)를 등록해 둔다. */
    private UUID applicant(boolean withPortfolio) {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> {
            player.nextYear(SchoolRules.AP_PER_TURN);
            player.nextYear(SchoolRules.AP_PER_TURN);
            // 학기 집중 선택이 필요한 필수 이벤트를 미리 끝내 두어 턴 종료가 막히지 않게 한다.
            player.chooseSemesterFocus("P");
        });
        if (withPortfolio) {
            registeredProject(id, "REST_API_SERVER", 90);
        }
        return id;
    }

    private void registeredProject(UUID id, String projectCode, int quality) {
        PlayerProject project = PlayerProject.start(id, projectCode, 3, 49, 58);
        project.finish(quality, true);
        project.registerPortfolio();
        projectRepository.save(project);
    }

    private void strong(UUID id) {
        fixture.addStats(id, Map.of("P", 60, "B", 60, "C", 60, "A", 60, "F", 40, "I", 40));
    }

    private ApplicationMutation apply(UUID id, String companyCode) {
        return careerService.apply(id, fixture.version(id), UUID.randomUUID().toString(),
                new ApplyJobRequest(companyCode));
    }

    private ApplicationMutation step(UUID id, UUID applicationId, String step) {
        return careerService.proceedStep(id, applicationId, fixture.version(id), UUID.randomUUID().toString(),
                new JobStepRequest(step));
    }

    private String statusOf(UUID id, UUID applicationId) {
        return careerService.getApplications(id).stream()
                .filter(view -> view.id().equals(applicationId))
                .map(ApplicationView::status)
                .findFirst().orElseThrow();
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
