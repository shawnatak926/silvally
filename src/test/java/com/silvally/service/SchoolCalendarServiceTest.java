package com.silvally.service;

import com.silvally.dto.request.*;
import com.silvally.common.exception.BusinessException;
import com.silvally.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(SchoolFixture.class)
class SchoolCalendarServiceTest {
    @Autowired SchoolFixture fixture;
    @Autowired PlayerService players;
    @Autowired ActivityService events;
    @Test void semesterGoalCostsNoApAndAppliesOnRegularTurn() {
        UUID id = players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("학기목표", null)).id();
        fixture.modify(id, p -> p.startSchool(3));
        assertThat(events.getEvents(id)).extracting(e -> e.code()).contains("FOCUS_Y1_S1");
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOf(BusinessException.class);
        long version = fixture.version(id);
        var selected = choose(id, "FOCUS_Y1_S1", "PROGRAMMING");
        assertThat(selected.spentAp()).isZero();
        assertThat(selected.player().remainingAp()).isEqualTo(3);
        assertThat(selected.statDelta()).isEmpty();
        assertThat(selected.player().version()).isEqualTo(version + 1);
        int p = fixture.milli(id, "P");
        fixture.advance(id);
        assertThat(fixture.milli(id, "P") - p).isEqualTo(500);
        assertThat(events.getEvent(id, "FOCUS_Y1_S1").choices()).allMatch(c -> !c.enabled());
    }
    com.silvally.dto.response.MutationResult chooseWithTopic(UUID id, String event, String choice, String topic) {
        return events.chooseEvent(id, event, fixture.version(id), UUID.randomUUID().toString(),
                ChooseEventRequest.withNarsha(choice, new NarshaInput("CUSTOM", topic, null)));
    }
    com.silvally.dto.response.MutationResult choose(UUID id, String event, String choice) {
        return events.chooseEvent(id, event, fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest(choice));
    }
    @Test void summerAndWinterFollowTwelveRegularTurnsAndCampAddsTwoSeparatePractices() {
        UUID id = fixture.enrolledStudent();
        fixture.advanceTimes(id, 11);
        assertThat(fixture.load(id).getTurn()).isEqualTo(12);
        assertThat(fixture.advance(id).resultCode()).isEqualTo("VACATION_REQUIRED");
        assertThat(fixture.load(id).getTurn()).isEqualTo(12);
        assertThatThrownBy(() -> fixture.advance(id)).isInstanceOf(BusinessException.class);
        assertThat(choose(id, "VACATION_Y1_SUMMER", "CAMP").resultCode()).isEqualTo("CAMP_READY");
        assertThat(fixture.load(id).getPracticeRemaining()).isEqualTo(2);
        var before = fixture.milli(id, "P");
        var one = fixture.advance(id);
        assertThat(one.player().turn()).isEqualTo(12);
        assertThat(one.statDelta()).containsEntry("C", 1);
        assertThat(fixture.milli(id, "P")).isEqualTo(before + 500);
        assertThat(fixture.load(id).getPracticeRemaining()).isEqualTo(1);
        assertThat(fixture.advance(id).player().turn()).isEqualTo(13);
        assertThat(fixture.milli(id, "P")).isEqualTo(before + 1000);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).containsExactly("FOCUS_Y1_S2");
        choose(id, "FOCUS_Y1_S2", "COMMUNICATION");
        fixture.advanceTimes(id, 11);
        assertThat(fixture.load(id).getTurn()).isEqualTo(24);
        assertThat(fixture.advance(id).resultCode()).isEqualTo("VACATION_REQUIRED");
        var yearEnd = choose(id, "VACATION_Y1_WINTER", "REST");
        assertThat(yearEnd.player().year()).isEqualTo(2);
        assertThat(yearEnd.player().turn()).isEqualTo(1);
        assertThat(yearEnd.details()).containsEntry("pendingChoice", "MAJOR");
    }
    @Test void narshaMilestonesAndTopThreeSoftwarewaveEligibilityFollowSavedRank() {
        UUID id = fixture.enrolledStudent();
        setPeriod(id, 1, 8);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).contains("NARSHA_START");
        chooseWithTopic(id, "NARSHA_START", "JOIN", "1학년 팀 프로젝트");
        setPeriod(id, 1, 22);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).doesNotContain("NARSHA_WRAP");
        setPeriod(id, 1, 23); choose(id, "NARSHA_WRAP", "SUBMIT");
        setPeriod(id, 2, 2); chooseWithTopic(id, "NARSHA_RESUME", "RESUME", "2학년 팀 프로젝트");
        fixture.addStats(id, Map.of("P", 100, "B", 100, "C", 100));
        setPeriod(id, 2, 22);
        var result = choose(id, "NARSHA_RANK", "CHECK");
        assertThat(result.details()).containsEntry("rank", 1);
        assertThat(fixture.load(id).getNarshaRank()).isEqualTo(1);
        setPeriod(id, 2, 23);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).contains("SOFTWAREWAVE");
        choose(id, "SOFTWAREWAVE", "ATTEND");
        assertThat(fixture.load(id).isSoftwarewaveAttended()).isTrue();
    }
    @Test void lowerRankCannotChooseSoftwarewaveAndRankCannotBeResubmitted() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, p -> { p.setNarshaStage("RESUMED"); p.getStats().apply(Map.of("P", -100, "B", -100, "C", -100)); });
        setPeriod(id, 2, 22);
        choose(id, "NARSHA_RANK", "CHECK");
        assertThat(fixture.load(id).getNarshaRank()).isEqualTo(11);
        assertThatThrownBy(() -> choose(id, "NARSHA_RANK", "CHECK")).isInstanceOf(BusinessException.class);
        setPeriod(id, 2, 23);
        assertThat(events.getEvents(id)).extracting(e -> e.code()).doesNotContain("SOFTWAREWAVE");
        assertThatThrownBy(() -> choose(id, "SOFTWAREWAVE", "ATTEND")).isInstanceOf(BusinessException.class);
    }
    void setPeriod(UUID id, int year, int turn) {
        fixture.modify(id, p -> {
            org.springframework.test.util.ReflectionTestUtils.setField(p, "year", year);
            org.springframework.test.util.ReflectionTestUtils.setField(p, "turn", turn);
            p.chooseSemesterFocus("C");
        });
    }
}
