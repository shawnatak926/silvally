package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.dto.response.HistoryPage;
import com.silvally.dto.response.PlayerView;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class HistoryServiceTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CommandLogRepository commandLogRepository;

    @Autowired
    private LifeNarrativeRepository narrativeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("입학 장면 선택 결과는 장면 ID·선택·결과와 서사가 회차 이력에 남는다")
    void admissionChoice_recordedWithNarrative() {
        UUID id = newPlayer();

        activityService.chooseEvent(id, "AD01", version(id), UUID.randomUUID().toString(),
                new ChooseEventRequest("GENERAL"));

        HistoryPage page = playerService.getHistory(id, 0, 20);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.kind()).isEqualTo("EVENT");
            assertThat(item.subjectCode()).isEqualTo("AD01");
            assertThat(item.choiceCode()).isEqualTo("GENERAL");
            assertThat(item.resultCode()).isEqualTo("SELECTED");
            assertThat(item.narrativeStatus()).isEqualTo("SAVED");
            assertThat(item.narrative()).contains("입학 전형 선택", "일반전형");
        });
        assertThat(page.nextAfterId()).isNull();
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 장면 선택을 재시도해도 이력과 서사는 한 번만 저장된다")
    void retriedChoice_savedOnce() {
        UUID id = newPlayer();
        long version = version(id);
        String key = UUID.randomUUID().toString();

        activityService.chooseEvent(id, "AD01", version, key, new ChooseEventRequest("GENERAL"));
        activityService.chooseEvent(id, "AD01", version, key, new ChooseEventRequest("GENERAL"));

        assertThat(sceneLogCount(id)).isEqualTo(1);
        assertThat(narrativeCount(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 장면 결과를 다시 기록해도 command_log와 서사는 하나만 남는다")
    void recordSceneTwice_savedOnce() {
        UUID id = newPlayer();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        SceneRecord scene = new SceneRecord("AD01", "GENERAL", "SELECTED", "{}");

        Long first = tx.execute(s -> historyService.recordScene(playerRepository.findById(id).orElseThrow(), scene).getId());
        Long second = tx.execute(s -> historyService.recordScene(playerRepository.findById(id).orElseThrow(), scene).getId());

        assertThat(second).isEqualTo(first);
        assertThat(sceneLogCount(id)).isEqualTo(1);
        assertThat(narrativeCount(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("회차 이력은 id 커서로 limit만큼 나눠 조회하고 다음 커서를 돌려준다")
    void history_cursorPagination() {
        UUID id = newPlayer();
        for (int i = 0; i < 3; i++) {
            playerService.rename(id, version(id), UUID.randomUUID().toString(), new RenamePlayerRequest("name" + i));
        }

        HistoryPage first = playerService.getHistory(id, 0, 2);
        assertThat(first.items()).hasSize(2).allSatisfy(item -> {
            assertThat(item.kind()).isEqualTo("RENAME");
            assertThat(item.resultCode()).isEqualTo("OK");
            assertThat(item.narrativeStatus()).isNull();
        });
        assertThat(first.nextAfterId()).isEqualTo(first.items().get(1).logId());

        HistoryPage second = playerService.getHistory(id, first.nextAfterId(), 2);
        assertThat(second.items()).hasSize(1);
        assertThat(second.items().get(0).logId()).isGreaterThan(first.nextAfterId());
        assertThat(second.nextAfterId()).isNull();
    }

    @Test
    @DisplayName("없는 회차의 이력을 조회하면 NOT_FOUND")
    void history_unknownPlayer_notFound() {
        assertThatThrownBy(() -> playerService.getHistory(UUID.randomUUID(), 0, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private UUID newPlayer() {
        PlayerView view = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("history", null));
        return view.id();
    }

    private long version(UUID id) {
        return playerRepository.findById(id).orElseThrow().getVersion();
    }

    private long sceneLogCount(UUID id) {
        return commandLogRepository.findAll().stream()
                .filter(log -> log.getPlayerId().equals(id) && "AD01".equals(log.getSubjectCode()))
                .count();
    }

    private long narrativeCount(UUID id) {
        return narrativeRepository.findAll().stream().filter(n -> n.getPlayerId().equals(id)).count();
    }
}
