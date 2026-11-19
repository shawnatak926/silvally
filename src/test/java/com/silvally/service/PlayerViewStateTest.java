package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.response.PlayerView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SchoolFixture.class)
class PlayerViewStateTest {

    @Autowired SchoolFixture fixture;
    @Autowired PlayerService players;
    @Autowired ActivityService events;

    @Test
    void admissionEligibilityListsEveryTrackFromSavedSeed() {
        PlayerView created = players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("전형", null));

        assertThat(created.admissionEligibility()).containsOnlyKeys("GENERAL", "MEISTER", "LOCAL", "SOCIAL")
                .containsEntry("GENERAL", true);
        var saved = fixture.load(created.id());
        created.admissionEligibility().forEach((track, eligible) -> assertThat(saved.eligible(track)).isEqualTo(eligible));
        assertThat(players.get(created.id()).admissionEligibility()).isEqualTo(created.admissionEligibility());
    }

    @Test
    void schoolStateFollowsSemesterFocusVacationCampAndYearChoice() {
        UUID id = players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("상태", null)).id();
        fixture.modify(id, p -> p.startSchool(SchoolRules.AP_PER_TURN));
        assertThat(players.get(id).school().semesterFocus()).isNull();

        var focused = choose(id, "FOCUS_Y1_S1", "ACADEMIC");
        assertThat(focused.player().school().semesterFocus()).isEqualTo("A");

        fixture.advanceTimes(id, 11);
        var vacation = fixture.advance(id).player().school();
        assertThat(vacation.vacationPending()).isTrue();
        assertThat(vacation.examPreparation()).isZero();

        var camp = choose(id, "VACATION_Y1_SUMMER", "CAMP").player().school();
        assertThat(camp.vacationPending()).isFalse();
        assertThat(camp.practiceRemaining()).isEqualTo(2);
        fixture.advance(id);
        var secondSemester = fixture.advance(id).player();
        assertThat(secondSemester.turn()).isEqualTo(13);
        assertThat(secondSemester.school().practiceRemaining()).isZero();
        assertThat(secondSemester.school().semesterFocus()).isNull();

        fixture.modify(id, p -> p.requireChoice("MAJOR"));
        assertThat(players.get(id).school().pendingChoice()).isEqualTo("MAJOR");
    }

    @Test
    void narshaStageAndRankAreReadableFromPlayerView() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> { p.setNarshaStage("RESUMED"); p.rankNarsha(2); p.attendSoftwarewave(); });

        var school = players.get(id).school();
        assertThat(school.narshaStage()).isEqualTo("RANKED");
        assertThat(school.narshaRank()).isEqualTo(2);
        assertThat(school.softwarewaveAttended()).isTrue();
    }

    private com.silvally.dto.response.MutationResult choose(UUID id, String event, String choice) {
        return events.chooseEvent(id, event, fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest(choice));
    }
}
