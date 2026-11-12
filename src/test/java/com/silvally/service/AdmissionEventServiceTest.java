package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.Player;
import com.silvally.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class AdmissionEventServiceTest {
    @Autowired ActivityService service;
    @Autowired PlayerRepository players;
    @Autowired ActivityRecordRepository records;
    @Autowired EventCommandRepository commands;
    @Autowired AdmissionPolicy policy;

    Player player(String track) {
        Player p = Player.admission("김일강", "OPEN");
        ReflectionTestUtils.setField(p, "seed", "admission-test");
        p.initializeEligibility();
        ReflectionTestUtils.setField(p, "meisterEligible", true);
        if (track != null) p.selectAdmissionTrack(track);
        return players.saveAndFlush(p);
    }
    MutationResult choose(Player p, String event, String choice) {
        return service.chooseEvent(p.getId(), event, p.getVersion(), UUID.randomUUID().toString(), new ChooseEventRequest(choice));
    }
    @Test void qualificationPersistsAndUnsupportedTrackIsRejected() {
        Player p = player(null);
        ReflectionTestUtils.setField(p, "localEligible", false);
        players.saveAndFlush(p);
        assertThat(service.getEvent(p.getId(), "AD01").choices()).anySatisfy(c -> {
            assertThat(c.code()).isEqualTo("GENERAL"); assertThat(c.enabled()).isTrue();
        });
        for (int i = 0; i < 3; i++) assertThat(players.findById(p.getId()).orElseThrow().eligible("MEISTER")).isTrue();
        assertThatThrownBy(() -> choose(p, "AD01", "LOCAL"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOCKED));
        assertThat(records.findByPlayerIdOrderByIdAsc(p.getId())).isEmpty();
        assertThat(players.findById(p.getId()).orElseThrow().getVersion()).isEqualTo(p.getVersion());
    }
    @Test void specialFailureUsesSubmittedDocumentsOnceAndContinuesOriginalStage() {
        Player p = player("MEISTER");
        var result = choose(p, "AD02", "ACADEMIC");
        assertThat(result.resultCode()).isEqualTo("SHIELD_PASSED");
        assertThat(result.player().phase()).isEqualTo("ADMISSION_2");
        assertThat(result.player().admissionTrack()).isEqualTo("MEISTER");
        var decision = (AdmissionPolicy.Decision) result.details().get("decision");
        assertThat(decision.limit()).isEqualTo(144);
        assertThat(decision.score()).isEqualTo(50);
        assertThat(decision.generalScore()).isEqualTo(75);
        assertThat(service.getEvents(p.getId())).extracting(e -> e.code()).containsExactly("AD03");
        assertThatThrownBy(() -> choose(players.findById(p.getId()).orElseThrow(), "AD02", "ACADEMIC"))
                .isInstanceOf(BusinessException.class);
        assertThat(records.findByPlayerIdOrderByIdAsc(p.getId())).hasSize(1);
    }
    @Test void shieldFailureEndsRunAndBlocksNextStage() {
        Player p = player("MEISTER");
        assertThat(choose(p, "AD02", "BASIC").resultCode()).isEqualTo("SHIELD_FAILED");
        Player ended = players.findById(p.getId()).orElseThrow();
        assertThat(ended.getPhase().name()).isEqualTo("ENDED");
        assertThat(service.getEvents(p.getId())).isEmpty();
        assertThatThrownBy(() -> choose(ended, "AD03", "PREPARED")).isInstanceOf(BusinessException.class);
    }
    @Test void replayReturnsFirstResponseAndRejectsPayloadOrVersionConflict() {
        Player p = player(null);
        var request = new ChooseEventRequest("GENERAL");
        var first = service.chooseEvent(p.getId(), "AD01", p.getVersion(), "retry", request);
        var replay = service.chooseEvent(p.getId(), "AD01", p.getVersion(), "retry", request);
        assertThat(replay.player()).isEqualTo(first.player());
        assertThatThrownBy(() -> service.chooseEvent(p.getId(), "AD01", p.getVersion(), "retry", new ChooseEventRequest("MEISTER")))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        assertThatThrownBy(() -> choose(p, "AD02", "BALANCED"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        assertThat(records.findByPlayerIdOrderByIdAsc(p.getId())).hasSize(1);
    }
    @Test void finalAdmissionKeeps72PlacesAndLeadsToBeforeSchool() {
        Player p = player("GENERAL");
        for (String event : new String[]{"AD02", "AD03", "AD04", "AD05"}) {
            var result = choose(p, event, event.equals("AD02") ? "BALANCED" : "PREPARED");
            if (event.equals("AD05")) {
                assertThat(((AdmissionPolicy.Decision) result.details().get("decision")).limit()).isEqualTo(72);
                assertThat(result.player().phase()).isEqualTo("BEFORE_SCHOOL");
                assertThat(result.player().beforeSchoolDay()).isEqualTo(1);
            }
            p = players.findById(p.getId()).orElseThrow();
        }
    }
    @Test void deterministicDrawHasIndependentGameProbabilities() {
        int[] counts = new int[3];
        String[] tracks = {"MEISTER", "LOCAL", "SOCIAL"};
        for (int i = 0; i < 10000; i++) {
            Player p = Player.admission("추첨", "OPEN");
            ReflectionTestUtils.setField(p, "seed", "run-" + i);
            for (int j = 0; j < 3; j++) if (p.eligible(tracks[j])) counts[j]++;
            assertThat(p.eligible("GENERAL")).isTrue();
        }
        assertThat(counts[0]).isBetween(6700, 7300);
        assertThat(counts[1]).isBetween(1700, 2300);
        assertThat(counts[2]).isBetween(4700, 5300);
        assertThat(policy.decide("ties", 1, "GENERAL", 35, 35).rank()).isEqualTo(288);
        assertThat(policy.decide("ties", 2, "GENERAL", 35, 35).rank()).isEqualTo(144);
    }
}
