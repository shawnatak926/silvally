package com.silvally.service;

import com.silvally.dto.request.*;
import com.silvally.entity.*;
import com.silvally.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(SchoolFixture.class)
class GameEditionTest {
    @Autowired PlayerService players;
    @Autowired ActivityService events;
    @Autowired CareerPreferenceService careers;
    @Autowired SchoolFixture fixture;
    @Autowired RecruitmentRules hiring;
    @Test void mildCreationRecommendationAndIdempotencyStartAtYearOneWithoutAdmission() {
        String key = UUID.randomUUID().toString(); var request = new CreatePlayerRequest("순한맛", null, "MILD");
        var created = players.create(key, request);
        assertThat(players.create(key, request)).isEqualTo(created);
        assertThat(created.edition()).isEqualTo("MILD"); assertThat(created.phase()).isEqualTo("CAREER_PICK");
        assertThat(created.year()).isEqualTo(1); assertThat(created.turn()).isEqualTo(1);
        assertThat(players.getHistory(created.id(), 0, 50).items()).isEmpty();
        var recommendations = careers.getPreference(created.id());
        assertThat(recommendations.recommendations()).hasSize(11);
        careers.select(created.id(), fixture.version(created.id()), UUID.randomUUID().toString(), new SelectCareerPreferenceRequest(recommendations.recommendations().getFirst().roleCode()));
        assertThat(fixture.load(created.id()).getPhase()).isEqualTo(Phase.SCHOOL);
        assertThatThrownBy(() -> players.create(key, new CreatePlayerRequest("순한맛", null, "SPICY"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("오류", null, "UNKNOWN"))).isInstanceOf(BusinessException.class);
    }
    @Test void mildCannotWithdrawAndSpicyKeepsCrisis() {
        for (String edition : List.of("MILD", "SPICY")) {
            var view = players.create(UUID.randomUUID().toString(), new CreatePlayerRequest("두 버전", null, edition));
            fixture.modify(view.id(), p -> { p.startSchool(3); p.chooseSemesterFocus("C"); p.getStats().apply(Map.of("S",100)); });
            var offered = events.getEvents(view.id()).getFirst();
            assertThat(offered.code()).isEqualTo(edition.equals("MILD") ? "STRESS_SUPPORT" : "SCHOOL_DOUBTS");
            if (edition.equals("MILD")) {
                assertThat(offered.choices()).noneMatch(c -> c.code().contains("WITHDRAWAL"));
                assertThatThrownBy(() -> events.chooseEvent(view.id(), "SCHOOL_DOUBTS", fixture.version(view.id()), UUID.randomUUID().toString(), new ChooseEventRequest("CONSIDER_WITHDRAWAL"))).isInstanceOf(BusinessException.class);
            }
        }
    }
    @Test void yearThreeCampNeverAppearsOrAcceptsRequestsInEitherVersion() {
        for (String edition : List.of("MILD", "SPICY")) {
            UUID id = fixture.enrolledStudent();
            fixture.modify(id, p -> {
                p.configureEdition(edition); p.startSchool(3);
                ReflectionTestUtils.setField(p, "year", 3); ReflectionTestUtils.setField(p, "turn",12);
                p.chooseSemesterFocus("C"); p.awaitVacation();
            });
            assertThat(events.getEvents(id).getFirst().choices()).noneMatch(c -> c.code().equals("CAMP"));
            assertThatThrownBy(() -> events.chooseEvent(id, "VACATION_Y3_SUMMER", fixture.version(id), UUID.randomUUID().toString(), new ChooseEventRequest("CAMP"))).isInstanceOf(BusinessException.class);
        }
    }
    @Test void mildHiringHasActualDocumentBonusAndDoesNotAwardCertificates() {
        UUID id = fixture.enrolledStudent(); var spicy = fixture.load(id);
        int before = hiring.documentScore(spicy, 40);
        fixture.modify(id, p -> p.configureEdition("MILD"));
        assertThat(hiring.documentScore(fixture.load(id),40)).isEqualTo(before + 15);
    }
}
