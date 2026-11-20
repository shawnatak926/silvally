package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.YearTransitionRequest;
import com.silvally.dto.response.YearTransitionView;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 학과·3학년 집중 수업 선택이 희망 직무를 정하거나 제한하지 않는지 검증한다. */
@SpringBootTest
class MajorTrackIndependenceTest {

    @Autowired
    private SchoolFixture fixture;
    @Autowired
    private SchoolTurnService turns;
    @Autowired
    private SchoolRules rules;

    @ParameterizedTest(name = "희망 직무 {0}, 학과 {1}, 집중 수업 {2}")
    @CsvSource({
            "GAME, SOFTWARE, NETWORK",
            "SECURITY, AI, GAME",
            "ANDROID, AI, WEB",
            "AI_NATIVE, SOFTWARE, MOBILE",
            "AI, SOFTWARE, AI"
    })
    @DisplayName("희망 직무와 다른 학과·집중 수업을 골라도 3학년 졸업 준비까지 진행하고 희망 직무는 그대로다")
    void anyMajorAndTrack_reachesGraduationWithoutChangingCareer(String career, String major, String track) {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "initialCareerCode", career);
            player.changeCareer(career);
        });

        int regular = 0;
        while (fixture.load(id).getPhase() == Phase.SCHOOL && regular < 73) {
            Player player = fixture.load(id);
            if (player.getPendingChoice() != null) {
                String code = player.getPendingChoice().equals("MAJOR") ? major : track;
                turns.chooseYearTransition(id, player.getVersion(), UUID.randomUUID().toString(),
                        new YearTransitionRequest(code));
            }
            fixture.completeCalendarChoices(id);
            player = fixture.load(id);
            if (player.getPhase() != Phase.SCHOOL) {
                break;
            }
            fixture.advance(id);
            regular++;
            fixture.completeCalendarChoices(id);
        }

        Player graduated = fixture.load(id);
        assertThat(regular).isEqualTo(72);
        assertThat(graduated.getPhase()).isEqualTo(Phase.GRAD_READY);
        assertThat(graduated.getMajor()).isEqualTo(major);
        assertThat(graduated.getTrack()).isEqualTo(track);
        assertThat(graduated.getInitialCareerCode()).isEqualTo(career);
        assertThat(graduated.getCurrentCareerCode()).isEqualTo(career);
    }

    @Test
    @DisplayName("학과 선택지는 소프트웨어개발과·인공지능소프트웨어과로 표시하고 직무를 제한하지 않는다는 안내를 준다")
    void majorOptions_useSchoolNamesAndNotice() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> player.requireChoice("MAJOR"));

        YearTransitionView view = turns.getYearTransition(id);

        assertThat(view.options()).extracting(YearTransitionView.Option::code).containsExactlyInAnyOrder("SOFTWARE", "AI");
        assertThat(view.options()).extracting(YearTransitionView.Option::title)
                .containsExactlyInAnyOrder("소프트웨어개발과", "인공지능소프트웨어과");
        assertThat(view.notice()).contains("희망 직무").contains("제한하지 않습니다");
    }

    @Test
    @DisplayName("3학년 집중 수업은 게임의 구분임을 밝히고 어떤 직무도 배제하지 않는다고 안내한다")
    void trackOptions_noticeSaysGameClassification() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> player.requireChoice("TRACK"));

        YearTransitionView view = turns.getYearTransition(id);

        assertThat(view.options()).hasSize(5);
        assertThat(view.notice()).contains("게임에서 나눈 구분").contains("교육과정 전체가 아닙니다").contains("배제되지 않습니다");
    }

    @Test
    @DisplayName("이전 표시의 학과 코드는 새로 고를 수 없지만 저장된 회차의 성장에는 그대로 쓰인다")
    void legacyMajor_notSelectableButStillGrows() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> {
            player.nextYear(SchoolRules.AP_PER_TURN);
            player.requireChoice("MAJOR");
        });
        long version = fixture.version(id);

        assertThatThrownBy(() -> turns.chooseYearTransition(id, version, UUID.randomUUID().toString(),
                new YearTransitionRequest("SECURITY")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        assertThat(fixture.load(id).getMajor()).isNull();

        fixture.modify(id, player -> ReflectionTestUtils.setField(player, "major", "SECURITY"));
        assertThat(rules.focusStats(fixture.load(id))).containsExactly("B", "A");
    }
}
