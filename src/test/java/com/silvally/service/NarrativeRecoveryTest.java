package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.NarrativeRetryResult;
import com.silvally.entity.LifeNarrative;
import com.silvally.entity.NarrativeStatus;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
class NarrativeRecoveryTest {

    @MockitoSpyBean
    private NarrativeGenerator generator;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private NarrativeService narrativeService;

    @Autowired
    private NarrativeRecoveryService recoveryService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private LifeNarrativeRepository narrativeRepository;

    @AfterEach
    void resetGenerator() {
        reset(generator);
    }

    @Test
    @DisplayName("AI 서사 생성이 실패해도 게임 진행과 이력은 저장되고 서사는 FAILED로 남는다")
    void generationFailure_keepsGameProgressAndMarksFailed() {
        doThrow(new IllegalStateException("AI timeout")).when(generator).generate(any());
        UUID id = newPlayer();

        MutationResult result = chooseGeneral(id);

        assertThat(result.player().admissionTrack()).isEqualTo("GENERAL");
        assertThat(playerRepository.findById(id).orElseThrow().getAdmissionTrack()).isEqualTo("GENERAL");
        LifeNarrative narrative = narrativeOf(id);
        assertThat(narrative.getStatus()).isEqualTo(NarrativeStatus.FAILED);
        assertThat(narrative.getAttempts()).isEqualTo(1);
        assertThat(narrative.getLastError()).contains("AI timeout");
        assertThat(playerService.getHistory(id, 0, 20).items().get(0).narrativeStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("실패한 서사는 재시도 API로 다시 생성되고, 저장된 뒤에는 재시도 대상에서 빠진다")
    void failedNarrative_recoveredByRetry() {
        doThrow(new IllegalStateException("AI timeout")).when(generator).generate(any());
        UUID id = newPlayer();
        chooseGeneral(id);
        reset(generator);

        NarrativeRetryResult retried = recoveryService.retry(id);

        assertThat(retried).isEqualTo(new NarrativeRetryResult(1, 1, 0));
        LifeNarrative narrative = narrativeOf(id);
        assertThat(narrative.getStatus()).isEqualTo(NarrativeStatus.SAVED);
        assertThat(narrative.getText()).contains("일반전형");
        assertThat(narrative.getLastError()).isNull();
        assertThat(recoveryService.retry(id)).isEqualTo(new NarrativeRetryResult(0, 0, 0));
    }

    @Test
    @DisplayName("이미 저장된 서사는 다시 생성 요청이 와도 본문과 시도 횟수가 바뀌지 않는다")
    void savedNarrative_writtenOnlyOnce() {
        UUID id = newPlayer();
        chooseGeneral(id);
        LifeNarrative saved = narrativeOf(id);

        narrativeService.write(saved.getId());

        LifeNarrative after = narrativeOf(id);
        assertThat(after.getStatus()).isEqualTo(NarrativeStatus.SAVED);
        assertThat(after.getText()).isEqualTo(saved.getText());
        assertThat(after.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("최대 시도 횟수만큼 실패한 서사는 재시도 대상에서 제외된다")
    void exhaustedNarrative_excludedFromRetry() {
        doThrow(new IllegalStateException("AI down")).when(generator).generate(any());
        UUID id = newPlayer();
        chooseGeneral(id);
        for (int i = 1; i < NarrativeRecoveryService.MAX_ATTEMPTS; i++) {
            recoveryService.retry(id);
        }

        assertThat(narrativeOf(id).getAttempts()).isEqualTo(NarrativeRecoveryService.MAX_ATTEMPTS);
        assertThat(recoveryService.retry(id)).isEqualTo(new NarrativeRetryResult(0, 0, 0));
    }

    private UUID newPlayer() {
        return playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("story", null)).id();
    }

    private MutationResult chooseGeneral(UUID id) {
        long version = playerRepository.findById(id).orElseThrow().getVersion();
        return activityService.chooseEvent(id, "AD01", version, UUID.randomUUID().toString(),
                new ChooseEventRequest("GENERAL"));
    }

    private LifeNarrative narrativeOf(UUID id) {
        return narrativeRepository.findAll().stream()
                .filter(n -> n.getPlayerId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
