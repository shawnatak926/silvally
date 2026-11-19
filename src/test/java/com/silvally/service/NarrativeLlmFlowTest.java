package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.response.HistoryPage;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.NarrativeRetryResult;
import com.silvally.entity.LifeNarrative;
import com.silvally.entity.NarrativeSource;
import com.silvally.entity.NarrativeStatus;
import com.silvally.entity.Player;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NarrativeLlmFlowTest {

    private static final String API_KEY = "flow-secret-key-5678";
    private static final String NARRATIVE = "원서를 내던 날, 나는 일반전형 칸에 조심스럽게 체크했다. 담임은 \"잘 골랐다\"며 고개를 끄덕였다.";
    private static final HttpServer SERVER = startServer();
    private static final AtomicInteger STATUS = new AtomicInteger(200);
    private static final AtomicReference<String> BODY = new AtomicReference<>();

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private NarrativeRecoveryService recoveryService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private LifeNarrativeRepository narrativeRepository;

    @DynamicPropertySource
    static void llmProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:narrative-llm;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("silvally.narrative.llm.api-key", () -> API_KEY);
        registry.add("silvally.narrative.llm.base-url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort() + "/v1");
        registry.add("silvally.narrative.llm.timeout", () -> "5s");
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @BeforeEach
    void succeedByDefault() {
        STATUS.set(200);
        BODY.set(completion(NARRATIVE));
    }

    @Test
    @DisplayName("LLM 서사는 커밋 후 비동기로 저장되고 이력에 출처 LLM과 응답 모델이 표시된다")
    void llmNarrative_savedWithSourceAndModel() {
        UUID id = newPlayer();

        MutationResult result = chooseGeneral(id);

        assertThat(result.player().admissionTrack()).isEqualTo("GENERAL");
        LifeNarrative narrative = awaitNarrative(id, n -> n.getStatus() == NarrativeStatus.SAVED);
        assertThat(narrative.getText()).isEqualTo(NARRATIVE);
        assertThat(narrative.getSource()).isEqualTo(NarrativeSource.LLM);
        assertThat(narrative.getModel()).isEqualTo("k-exaone_v2.5-served");
        HistoryPage.Item item = playerService.getHistory(id, 0, 20).items().get(0);
        assertThat(item.narrativeStatus()).isEqualTo("SAVED");
        assertThat(item.narrativeSource()).isEqualTo("LLM");
        assertThat(item.narrativeModel()).isEqualTo("k-exaone_v2.5-served");
        assertThat(item.narrative()).isEqualTo(NARRATIVE);
    }

    @Test
    @DisplayName("401 실패에도 게임 결과는 유지되고 대체 서사가 표시되며, 재시도는 같은 서사를 LLM 결과로 한 번만 갱신한다")
    void unauthorized_keepsGameStateAndRecoversByRetry() {
        STATUS.set(401);
        BODY.set("{\"error\":{\"message\":\"invalid api key\",\"type\":\"invalid_request_error\"}}");
        UUID id = newPlayer();

        chooseGeneral(id);

        LifeNarrative failed = awaitNarrative(id, n -> n.getStatus() == NarrativeStatus.FAILED);
        Player player = playerRepository.findById(id).orElseThrow();
        assertThat(player.getAdmissionTrack()).isEqualTo("GENERAL");
        assertThat(failed.getSource()).isEqualTo(NarrativeSource.FALLBACK);
        assertThat(failed.getText()).contains("일반전형");
        assertThat(failed.getLastError()).doesNotContain(API_KEY);
        HistoryPage.Item item = playerService.getHistory(id, 0, 20).items().get(0);
        assertThat(item.narrativeStatus()).isEqualTo("FAILED");
        assertThat(item.narrativeSource()).isEqualTo("FALLBACK");
        long versionBeforeRetry = player.getVersion();

        succeedByDefault();
        NarrativeRetryResult retried = recoveryService.retry(id);

        assertThat(retried).isEqualTo(new NarrativeRetryResult(1, 1, 0));
        List<LifeNarrative> narratives = narrativesOf(id);
        assertThat(narratives).singleElement().satisfies(n -> {
            assertThat(n.getStatus()).isEqualTo(NarrativeStatus.SAVED);
            assertThat(n.getSource()).isEqualTo(NarrativeSource.LLM);
            assertThat(n.getText()).isEqualTo(NARRATIVE);
            assertThat(n.getAttempts()).isEqualTo(2);
        });
        assertThat(playerRepository.findById(id).orElseThrow().getVersion()).isEqualTo(versionBeforeRetry);
        assertThat(recoveryService.retry(id)).isEqualTo(new NarrativeRetryResult(0, 0, 0));
    }

    @Test
    @DisplayName("JSON이 아닌 LLM 응답도 게임 결과를 롤백하지 않고 대체 서사로 남는다")
    void malformedResponse_keepsGameState() {
        BODY.set("not-json");
        UUID id = newPlayer();

        MutationResult result = chooseGeneral(id);

        LifeNarrative failed = awaitNarrative(id, n -> n.getStatus() == NarrativeStatus.FAILED);
        assertThat(failed.getSource()).isEqualTo(NarrativeSource.FALLBACK);
        assertThat(playerRepository.findById(id).orElseThrow().getVersion()).isEqualTo(result.player().version());
        assertThat(playerRepository.findById(id).orElseThrow().getAdmissionTrack()).isEqualTo("GENERAL");
    }

    private UUID newPlayer() {
        return playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("llm", null)).id();
    }

    private MutationResult chooseGeneral(UUID id) {
        long version = playerRepository.findById(id).orElseThrow().getVersion();
        return activityService.chooseEvent(id, "AD01", version, UUID.randomUUID().toString(),
                new ChooseEventRequest("GENERAL"));
    }

    private List<LifeNarrative> narrativesOf(UUID id) {
        return narrativeRepository.findAll().stream().filter(n -> n.getPlayerId().equals(id)).toList();
    }

    private LifeNarrative awaitNarrative(UUID id, Predicate<LifeNarrative> condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            List<LifeNarrative> found = narrativesOf(id);
            if (found.size() == 1 && condition.test(found.get(0))) {
                return found.get(0);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("narrative not ready: " + narrativesOf(id));
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/v1/chat/completions", exchange -> {
                exchange.getRequestBody().readAllBytes();
                byte[] body = BODY.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(STATUS.get(), body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String completion(String content) {
        return """
                {"id":"chatcmpl-flow","object":"chat.completion","created":1763520000,
                 "model":"k-exaone_v2.5-served",
                 "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant","content":"%s"}}],
                 "usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}}
                """.formatted(content.replace("\"", "\\\""));
    }
}
