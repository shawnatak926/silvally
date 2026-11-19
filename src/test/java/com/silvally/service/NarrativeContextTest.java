package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.entity.LifeNarrative;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
class NarrativeContextTest {

    @MockitoSpyBean
    private NarrativeGenerator generator;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private NarrativeRecoveryService recoveryService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private LifeNarrativeRepository narrativeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void resetGenerator() {
        reset(generator);
    }

    @Test
    @DisplayName("재시도로 늦게 생성해도 서사 맥락은 장면이 기록된 시점의 상태를 쓴다")
    void delayedGeneration_usesStateAtSceneTime() {
        doThrow(new IllegalStateException("AI timeout")).when(generator).generate(any());
        UUID id = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("before", null)).id();
        chooseGeneral(id);
        playerService.rename(id, version(id), UUID.randomUUID().toString(), new RenamePlayerRequest("after"));
        reset(generator);

        recoveryService.retry(id);

        NarrativeScene scene = lastScene();
        assertThat(scene.context().playerName()).isEqualTo("before");
        assertThat(scene.context().admissionTrack()).isEqualTo("GENERAL");
        assertThat(scene.context().phase()).isEqualTo("ADMISSION_1");
        assertThat(scene.context().stats()).containsKeys("P", "A", "H", "EN");
        assertThat(scene.context().recentChoices()).isEmpty();
        assertThat(scene.previousNarratives()).isEmpty();
    }

    @Test
    @DisplayName("다음 장면 맥락에는 그보다 앞선 선택과 저장된 서사만 포함된다")
    void laterScene_includesOnlyEarlierChoicesAndNarratives() {
        UUID id = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("story", null)).id();
        chooseGeneral(id);
        String firstNarrative = narrativesOf(id).get(0).getText();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> historyService.recordScene(
                playerRepository.findById(id).orElseThrow(), new SceneRecord("X01", "PICK", "DONE", "{}")));

        NarrativeScene second = lastScene();
        assertThat(second.sceneId()).isEqualTo("X01");
        assertThat(second.previousNarratives()).containsExactly(firstNarrative);
        assertThat(second.context().recentChoices()).singleElement().satisfies(choice -> {
            assertThat(choice.sceneId()).isEqualTo("AD01");
            assertThat(choice.choiceCode()).isEqualTo("GENERAL");
            assertThat(choice.resultCode()).isEqualTo("SELECTED");
        });
        assertThat(narrativesOf(id)).allSatisfy(n -> assertThat(n.getContextJson()).isNotBlank());
    }

    private void chooseGeneral(UUID id) {
        activityService.chooseEvent(id, "AD01", version(id), UUID.randomUUID().toString(),
                new ChooseEventRequest("GENERAL"));
    }

    private long version(UUID id) {
        return playerRepository.findById(id).orElseThrow().getVersion();
    }

    private NarrativeScene lastScene() {
        ArgumentCaptor<NarrativeScene> captor = ArgumentCaptor.forClass(NarrativeScene.class);
        verify(generator, atLeastOnce()).generate(captor.capture());
        return captor.getValue();
    }

    private List<LifeNarrative> narrativesOf(UUID id) {
        return narrativeRepository.findAll().stream().filter(n -> n.getPlayerId().equals(id)).toList();
    }
}
