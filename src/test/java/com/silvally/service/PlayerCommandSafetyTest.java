package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.dto.response.PlayerView;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PlayerCommandSafetyTest {

    private static final int THREADS = 8;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CommandLogRepository commandLogRepository;

    @Test
    @DisplayName("같은 Idempotency-Key로 회차 생성을 재시도하면 처음 만든 회차를 그대로 반환한다")
    void create_retryWithSameKey_returnsSavedPlayer() {
        String key = UUID.randomUUID().toString();
        PlayerView first = playerService.create(key, new CreatePlayerRequest("retry", null));
        PlayerView second = playerService.create(key, new CreatePlayerRequest("retry", null));

        assertThat(second).isEqualTo(first);
        assertThat(first.version()).isZero();
        assertThat(first.applicationProfile()).isEqualTo("OPEN");
        assertThat(playerRepository.findByCreateKey(key)).isPresent();
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 다른 내용의 생성 요청이 오면 IDEMPOTENCY_CONFLICT")
    void create_sameKeyDifferentBody_conflicts() {
        String key = UUID.randomUUID().toString();
        playerService.create(key, new CreatePlayerRequest("first", null));

        assertThatThrownBy(() -> playerService.create(key, new CreatePlayerRequest("second", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 동시에 생성해도 회차는 하나만 저장된다")
    void create_concurrentSameKey_savesOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        long before = playerRepository.count();

        List<Result<PlayerView>> results = runConcurrently(
                () -> playerService.create(key, new CreatePlayerRequest("dup", null)));

        assertThat(results).allMatch(Result::succeeded);
        assertThat(results).extracting(r -> r.value().id()).containsOnly(results.get(0).value().id());
        assertThat(playerRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 수정을 재시도하면 저장된 응답을 반환하고 한 번만 반영한다")
    void rename_retryWithSameKey_appliesOnce() {
        PlayerView player = newPlayer();
        String key = UUID.randomUUID().toString();

        PlayerView first = playerService.rename(player.id(), 0, key, new RenamePlayerRequest("renamed"));
        PlayerView retry = playerService.rename(player.id(), 0, key, new RenamePlayerRequest("renamed"));

        assertThat(first.version()).isEqualTo(1);
        assertThat(retry).isEqualTo(first);
        assertThat(playerService.get(player.id()).version()).isEqualTo(1);
        assertThat(logCount(player.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("오래된 버전으로 수정하면 현재 버전을 담아 VERSION_CONFLICT")
    void rename_staleVersion_conflicts() {
        PlayerView player = newPlayer();
        playerService.rename(player.id(), 0, UUID.randomUUID().toString(), new RenamePlayerRequest("v1"));

        assertThatThrownBy(() -> playerService.rename(
                player.id(), 0, UUID.randomUUID().toString(), new RenamePlayerRequest("stale")))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT);
                    assertThat(e.getDetails()).containsEntry("currentVersion", 1L);
                });
        assertThat(playerService.get(player.id()).name()).isEqualTo("v1");
    }

    @Test
    @DisplayName("같은 버전으로 서로 다른 커맨드가 동시에 오면 하나만 반영되고 나머지는 VERSION_CONFLICT")
    void command_concurrentSameVersion_onlyOneApplied() throws Exception {
        PlayerView player = newPlayer();

        List<Result<PlayerView>> results = runConcurrently(() -> playerService.rename(
                player.id(), 0, UUID.randomUUID().toString(), new RenamePlayerRequest("racer")));

        assertThat(results).filteredOn(Result::succeeded).hasSize(1);
        assertThat(results).filteredOn(r -> !r.succeeded())
                .allMatch(r -> r.error() == ErrorCode.VERSION_CONFLICT)
                .hasSize(THREADS - 1);
        assertThat(playerService.get(player.id()).version()).isEqualTo(1);
        assertThat(logCount(player.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 Idempotency-Key 커맨드가 동시에 오면 모두 같은 응답을 받고 한 번만 저장된다")
    void command_concurrentSameKey_savedOnce() throws Exception {
        PlayerView player = newPlayer();
        String key = UUID.randomUUID().toString();

        List<Result<PlayerView>> results = runConcurrently(() -> playerService.rename(
                player.id(), 0, key, new RenamePlayerRequest("once")));

        assertThat(results).allMatch(Result::succeeded);
        assertThat(results).extracting(r -> r.value().version()).containsOnly(1L);
        assertThat(playerService.get(player.id()).version()).isEqualTo(1);
        assertThat(logCount(player.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("Idempotency-Key가 UUID가 아니면 VALIDATION_ERROR")
    void command_invalidKey_rejected() {
        PlayerView player = newPlayer();

        assertThatThrownBy(() -> playerService.rename(player.id(), 0, "not-a-uuid", new RenamePlayerRequest("x")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    @DisplayName("회차 삭제는 버전을 검증하고 회차와 커맨드 이력을 함께 지운다")
    void delete_removesPlayerAndLogs() {
        PlayerView player = newPlayer();
        playerService.rename(player.id(), 0, UUID.randomUUID().toString(), new RenamePlayerRequest("bye"));

        assertThatThrownBy(() -> playerService.delete(player.id(), 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));

        assertThat(playerService.delete(player.id(), 1).deleted()).isTrue();
        assertThat(playerRepository.findById(player.id())).isEmpty();
        assertThat(logCount(player.id())).isZero();
        assertThatThrownBy(() -> playerService.get(player.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private PlayerView newPlayer() {
        return playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("tester", null));
    }

    private long logCount(UUID playerId) {
        return commandLogRepository.findAll().stream()
                .filter(log -> log.getPlayerId().equals(playerId))
                .count();
    }

    private <T> List<Result<T>> runConcurrently(Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Result<T>>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return new Result<>(task.call(), null);
                    } catch (BusinessException e) {
                        return new Result<T>(null, e.getErrorCode());
                    }
                }));
            }
            start.countDown();
            List<Result<T>> results = new ArrayList<>();
            for (Future<Result<T>> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private record Result<T>(T value, ErrorCode error) {
        boolean succeeded() {
            return error == null;
        }
    }
}
