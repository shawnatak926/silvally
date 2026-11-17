package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.YearTransitionRequest;
import com.silvally.dto.response.CourseView;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.CourseStatus;
import com.silvally.repository.CourseEnrollmentRepository;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CertificateServiceTest {

    private static final String INFO = "INFO_CRAFTSMAN";
    private static final String ENGLISH = "ENGLISH_SPEAKING";

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private SchoolTurnService schoolTurnService;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private SchoolFixture fixture;

    @Test
    @DisplayName("능력치 수강 조건을 채우지 못하면 LOCKED와 부족한 조건을 안내한다")
    void enroll_statRequirementUnmet_locked() {
        UUID id = fixture.enrolledStudent();

        assertThatThrownBy(() -> enroll(id, INFO))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOCKED);
                    assertThat(e.getDetails().get("reasons").toString()).contains("P 25 이상");
                });
    }

    @Test
    @DisplayName("수강 가능 학년 전에는 신청할 수 없다")
    void enroll_beforeMinYear_locked() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("B", 50));

        CourseView linux = certificateService.getCourses(id).stream()
                .filter(c -> c.code().equals("LINUX_MASTER")).findFirst().orElseThrow();
        assertThat(linux.enrollable()).isFalse();
        assertThat(linux.reasons()).anyMatch(r -> r.contains("2학년부터"));
        assertError(() -> enroll(id, "LINUX_MASTER"), ErrorCode.LOCKED);
    }

    @Test
    @DisplayName("출석 2회를 서로 다른 턴에 마친 뒤 시험에 합격하면 자격증을 취득한다")
    void attendTwiceThenPass_earnsCertificate() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("P", 60, "A", 60));
        enroll(id, INFO);

        assertError(() -> exam(id, INFO), ErrorCode.WRONG_STEP);
        MutationResult first = attend(id, INFO);
        assertThat(first.spentAp()).isEqualTo(1);
        assertThat(first.player().remainingAp()).isEqualTo(SchoolRules.AP_PER_TURN - 1);
        assertError(() -> attend(id, INFO), ErrorCode.WRONG_STEP);

        fixture.advance(id);
        assertThat(attend(id, INFO).details()).containsEntry("attendanceCount", 2);
        assertError(() -> attend(id, INFO), ErrorCode.WRONG_STEP);

        MutationResult result = exam(id, INFO);
        assertThat(result.resultCode()).isEqualTo("PASSED");
        assertThat((Integer) result.details().get("score")).isGreaterThanOrEqualTo(60);
        assertThat(status(id, INFO)).isEqualTo(CourseStatus.PASSED);
        assertError(() -> enroll(id, INFO), ErrorCode.LOCKED);
    }

    @Test
    @DisplayName("시험 점수가 합격선보다 낮으면 FAILED로 기록된다")
    void lowScore_failed() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("P", 5, "A", -24));
        enroll(id, INFO);
        attend(id, INFO);
        fixture.advance(id);
        fixture.addStats(id, Map.of("A", -10));
        attend(id, INFO);

        MutationResult result = exam(id, INFO);

        assertThat(result.resultCode()).isEqualTo("FAILED");
        assertThat(status(id, INFO)).isEqualTo(CourseStatus.FAILED);
    }

    @Test
    @DisplayName("AP가 부족하면 출석할 수 없다")
    void attend_withoutAp_insufficient() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("P", 10));
        enroll(id, INFO);
        fixture.modify(id, player -> player.spendAp(SchoolRules.AP_PER_TURN));

        assertError(() -> attend(id, INFO), ErrorCode.INSUFFICIENT_AP);
    }

    @Test
    @DisplayName("학년이 끝날 때 시험까지 마치지 못한 수강은 만료된다")
    void unfinishedEnrollment_expiresAtYearEnd() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("P", 10));
        enroll(id, INFO);
        attend(id, INFO);

        MutationResult yearEnd = fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR);

        assertThat(yearEnd.details().get("expiredCourses")).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactly(INFO);
        assertThat(status(id, INFO)).isEqualTo(CourseStatus.EXPIRED);
    }

    @Test
    @DisplayName("유효기간 1년 자격증은 다음 학년까지 유지되고 그 다음 학년 전환 때 만료된다")
    void validityExpires_afterValidYears() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("C", 60, "A", 60));
        enroll(id, ENGLISH);
        attend(id, ENGLISH);
        fixture.advance(id);
        attend(id, ENGLISH);
        assertThat(exam(id, ENGLISH).resultCode()).isEqualTo("PASSED");

        fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR - 1);
        assertThat(status(id, ENGLISH)).isEqualTo(CourseStatus.PASSED);

        schoolTurnService.chooseYearTransition(id, fixture.version(id), UUID.randomUUID().toString(),
                new YearTransitionRequest("SOFTWARE"));
        MutationResult secondYearEnd = fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR);

        assertThat(secondYearEnd.details().get("expiredCourses")).asInstanceOf(InstanceOfAssertFactories.LIST).contains(ENGLISH);
        assertThat(status(id, ENGLISH)).isEqualTo(CourseStatus.EXPIRED);
    }

    private MutationResult enroll(UUID id, String code) {
        return certificateService.enroll(id, code, fixture.version(id), UUID.randomUUID().toString());
    }

    private MutationResult attend(UUID id, String code) {
        return certificateService.attend(id, code, fixture.version(id), UUID.randomUUID().toString());
    }

    private MutationResult exam(UUID id, String code) {
        return certificateService.takeExam(id, code, fixture.version(id), UUID.randomUUID().toString());
    }

    private CourseStatus status(UUID id, String code) {
        return enrollmentRepository.findByPlayerIdAndCourseCode(id, code).stream()
                .map(e -> e.getStatus()).reduce((a, b) -> b).orElseThrow();
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
