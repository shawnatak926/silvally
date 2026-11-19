package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.YearTransitionRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SchoolTurnServiceTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private SchoolTurnService schoolTurnService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private SchoolFixture fixture;

    @Test
    @DisplayName("재학 중이 아니면 턴을 진행할 수 없다")
    void advance_beforeSchool_wrongPhase() {
        UUID id = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("pre", null)).id();

        assertError(() -> fixture.advance(id), ErrorCode.WRONG_PHASE);
    }

    @Test
    @DisplayName("턴 종료 시 1학년 기초 능력치가 오르고 남은 AP만큼 H가 회복된다")
    void advance_growsBaseStatsAndRestores() {
        UUID id = fixture.enrolledStudent();
        Player before = fixture.load(id);

        MutationResult result = fixture.advance(id);

        Player after = fixture.load(id);
        assertThat(result.resultCode()).isEqualTo("TURN_ADVANCED");
        assertThat(result.statDelta()).containsEntry("P", 1).containsEntry("A", 1).containsEntry("H", 6);
        assertThat(after.getStats().getP()).isEqualTo(before.getStats().getP() + 1);
        assertThat(after.getTurn()).isEqualTo(2);
        assertThat(after.getAp()).isEqualTo(SchoolRules.AP_PER_TURN);
    }

    @Test
    @DisplayName("AP를 모두 쓰고 턴을 마치면 H가 2 감소한다")
    void advance_withoutAp_fatigue() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> player.spendAp(SchoolRules.AP_PER_TURN));

        MutationResult result = fixture.advance(id);

        assertThat(result.statDelta()).containsEntry("H", -2);
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 턴 종료를 재시도하면 한 턴만 진행된다")
    void advance_retry_appliesOnce() {
        UUID id = fixture.enrolledStudent();
        String key = UUID.randomUUID().toString();
        long version = fixture.version(id);

        MutationResult first = playerService.advanceTurn(id, version, key);
        MutationResult retry = playerService.advanceTurn(id, version, key);

        assertThat(retry).isEqualTo(first);
        assertThat(fixture.load(id).getTurn()).isEqualTo(2);
    }

    @Test
    @DisplayName("1학년 마지막 턴을 마치면 2학년이 되고 전공 선택 전에는 재학 요청이 막힌다")
    void yearEnd_requiresMajor_blocksSchoolRequests() {
        UUID id = fixture.enrolledStudent();

        MutationResult result = fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR);

        Player player = fixture.load(id);
        assertThat(result.resultCode()).isEqualTo("YEAR_ADVANCED");
        assertThat(result.details()).containsEntry("pendingChoice", "MAJOR");
        assertThat(player.getYear()).isEqualTo(2);
        assertThat(player.getTurn()).isEqualTo(1);

        assertThatThrownBy(() -> fixture.advance(id))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHOICE_REQUIRED);
                    assertThat(e.getDetails()).containsEntry("pendingChoice", "MAJOR");
                    assertThat(e.getDetails().get("options")).asInstanceOf(InstanceOfAssertFactories.LIST).contains("SOFTWARE", "SECURITY");
                });
        assertError(() -> certificateService.enroll(id, "INFO_CRAFTSMAN", fixture.version(id),
                UUID.randomUUID().toString()), ErrorCode.CHOICE_REQUIRED);
        assertThat(schoolTurnService.getYearTransition(id).options()).hasSize(3);
    }

    @Test
    @DisplayName("전공을 확정하면 진행이 풀리고 전공 능력치가 성장한다")
    void chooseMajor_unblocksAndChangesGrowth() {
        UUID id = fixture.enrolledStudent();
        fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR);

        assertError(() -> choose(id, "UNKNOWN"), ErrorCode.VALIDATION_ERROR);
        choose(id, "SOFTWARE");

        Player player = fixture.load(id);
        assertThat(player.getMajor()).isEqualTo("SOFTWARE");
        assertThat(player.getPendingChoice()).isNull();
        assertThat(fixture.advance(id).statDelta()).containsEntry("P", 1).containsEntry("B", 1)
                .doesNotContainKey("A");
        assertError(() -> choose(id, "SECURITY"), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("2학년을 마치면 트랙을 선택하고, 3학년을 마치면 졸업 준비 단계가 된다")
    void fullSchoolYears_trackThenGraduation() {
        UUID id = fixture.enrolledStudent();
        fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR);
        choose(id, "SOFTWARE");

        MutationResult secondYearEnd = fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR);
        assertThat(secondYearEnd.details()).containsEntry("pendingChoice", "TRACK");
        choose(id, "WEB");
        assertThat(fixture.advance(id).statDelta()).containsEntry("B", 2).containsEntry("F", 1);

        MutationResult thirdYearEnd = fixture.advanceTimes(id, SchoolRules.TURNS_PER_YEAR - 1);
        assertThat(thirdYearEnd.resultCode()).isEqualTo("GRADUATION_READY");
        assertThat(fixture.load(id).getPhase()).isEqualTo(Phase.GRAD_READY);
        assertError(() -> fixture.advance(id), ErrorCode.WRONG_PHASE);
    }

    private void choose(UUID id, String code) {
        schoolTurnService.chooseYearTransition(id, fixture.version(id), UUID.randomUUID().toString(),
                new YearTransitionRequest(code));
        fixture.completeCalendarChoices(id);
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
