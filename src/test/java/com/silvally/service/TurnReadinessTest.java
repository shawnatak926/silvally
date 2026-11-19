package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.response.TurnReadinessView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SchoolFixture.class)
class TurnReadinessTest {

    @Autowired SchoolFixture fixture;
    @Autowired PlayerService players;
    @Autowired ActivityService events;

    @Test
    void notEnrolledPlayerIsBlockedByPhase() {
        UUID id = players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("입학 전", null)).id();

        TurnReadinessView readiness = players.getTurnReadiness(id);

        assertThat(readiness.canAdvance()).isFalse();
        assertThat(readiness.blockers()).singleElement()
                .satisfies(b -> {
                    assertThat(b.type()).isEqualTo("PHASE");
                    assertThat(b.code()).isEqualTo("ADMISSION_1");
                });
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WRONG_PHASE));
    }

    @Test
    void requiredSemesterFocusAndVacationBlockUntilChosenAndCampTurnsStayAdvanceable() {
        UUID id = players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("필수 선택", null)).id();
        fixture.modify(id, p -> p.startSchool(SchoolRules.AP_PER_TURN));
        long version = fixture.version(id);

        assertBlockedBy(id, "REQUIRED_EVENT", "FOCUS_Y1_S1");
        assertThat(fixture.version(id)).isEqualTo(version);
        choose(id, "FOCUS_Y1_S1", "PROGRAMMING");
        assertThat(players.getTurnReadiness(id).canAdvance()).isTrue();

        fixture.advanceTimes(id, 11);
        assertThat(fixture.advance(id).resultCode()).isEqualTo("VACATION_REQUIRED");
        assertBlockedBy(id, "REQUIRED_EVENT", "VACATION_Y1_SUMMER");
        choose(id, "VACATION_Y1_SUMMER", "CAMP");

        TurnReadinessView camp = players.getTurnReadiness(id);
        assertThat(camp.canAdvance()).isTrue();
        assertThat(camp.turn()).isEqualTo(12);
        assertThat(camp.version()).isEqualTo(fixture.version(id));
    }

    @Test
    void yearTransitionChoiceListsOptionsAndBlocksBeforeEvents() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> p.requireChoice("MAJOR"));

        TurnReadinessView readiness = players.getTurnReadiness(id);

        assertThat(readiness.canAdvance()).isFalse();
        assertThat(readiness.blockers()).singleElement().satisfies(b -> {
            assertThat(b.type()).isEqualTo("YEAR_CHOICE");
            assertThat(b.code()).isEqualTo("MAJOR");
            assertThat(b.options()).isNotEmpty();
        });
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHOICE_REQUIRED));
    }

    @Test
    void stressSupportBlocksReadinessAndAdvanceUntilResolved() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> p.getStats().apply(Map.of("S", 60 - p.getStats().getS())));
        long version = fixture.version(id);

        assertBlockedBy(id, "REQUIRED_EVENT", "STRESS_SUPPORT");
        assertThat(fixture.version(id)).isEqualTo(version);
        String rest = events.getEvent(id, "STRESS_SUPPORT").choices().getFirst().code();
        choose(id, "STRESS_SUPPORT", rest);

        assertThat(fixture.load(id).getStats().getS()).isLessThan(60);
        assertThat(players.getTurnReadiness(id).canAdvance()).isTrue();
        fixture.advance(id);
    }

    @Test
    void readinessAgreesWithTurnAdvanceAcrossAFullSemesterAndVacation() {
        UUID id = fixture.enrolledStudent();
        for (int step = 0; step < 20; step++) {
            TurnReadinessView readiness = players.getTurnReadiness(id);
            if (readiness.canAdvance()) {
                fixture.advance(id);
                continue;
            }
            assertThatThrownBy(() -> fixture.advance(id)).isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHOICE_REQUIRED));
            String event = readiness.blockers().getFirst().code();
            choose(id, event, event.startsWith("VACATION_") ? "REST" : "COMMUNICATION");
        }
        assertThat(fixture.load(id).getTurn()).isGreaterThan(13);
    }

    private void assertBlockedBy(UUID id, String type, String code) {
        TurnReadinessView readiness = players.getTurnReadiness(id);
        assertThat(readiness.canAdvance()).isFalse();
        assertThat(readiness.blockers()).extracting(TurnReadinessView.Blocker::type, TurnReadinessView.Blocker::code)
                .contains(org.assertj.core.groups.Tuple.tuple(type, code));
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHOICE_REQUIRED));
    }

    private void choose(UUID id, String event, String choice) {
        events.chooseEvent(id, event, fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest(choice));
    }
}
