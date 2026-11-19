package com.silvally.service;

import com.silvally.config.NarrativeConfig;
import com.silvally.config.NarrativeLlmProperties;
import com.silvally.entity.NarrativeSource;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmNarrativeGeneratorTest {

    private static final String API_KEY = "test-secret-key-1234";
    private static final String NARRATIVE = "복도 끝 교실에서 나는 노트북을 열었다. 선배가 \"천천히 해도 돼\"라고 웃었다.";

    private final ObjectMapper json = JsonMapper.builder().build();
    private final List<String> requestBodies = new CopyOnWriteArrayList<>();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private final AtomicReference<Reply> reply = new AtomicReference<>();
    private HttpServer server;

    private record Reply(int status, String body, long delayMillis) {
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            Reply current = reply.get();
            try {
                Thread.sleep(current.delayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = current.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(current.status(), body.length);
                exchange.getResponseBody().write(body);
            } catch (IOException ignored) {
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("API 키가 없으면 템플릿 mock 생성기를, 있으면 LLM 생성기를 사용한다")
    void providerSelection_byApiKey() {
        NarrativeConfig config = new NarrativeConfig();

        assertThat(config.narrativeGenerator(properties(null, Duration.ofSeconds(5)), json))
                .isInstanceOf(TemplateNarrativeGenerator.class);
        assertThat(config.narrativeGenerator(properties(" ", Duration.ofSeconds(5)), json))
                .isInstanceOf(TemplateNarrativeGenerator.class);
        assertThat(config.narrativeGenerator(properties(API_KEY, Duration.ofSeconds(5)), json))
                .isInstanceOf(LlmNarrativeGenerator.class);
    }

    @Test
    @DisplayName("LLM 응답 본문과 실제 응답 모델을 출처 LLM으로 돌려주고 키는 Authorization 헤더로만 보낸다")
    void success_returnsTextAndResponseModel() {
        reply.set(new Reply(200, completion(NARRATIVE, "k-exaone_v2.5-0901"), 0));

        GeneratedNarrative result = generator(Duration.ofSeconds(5)).generate(scene("AD01", 1, "GENERAL"));

        assertThat(result.text()).isEqualTo(NARRATIVE);
        assertThat(result.source()).isEqualTo(NarrativeSource.LLM);
        assertThat(result.model()).isEqualTo("k-exaone_v2.5-0901");
        assertThat(authorizations).containsExactly("Bearer " + API_KEY);
        assertThat(requestBodies.get(0)).doesNotContain(API_KEY).contains("k-exaone_v2.5");
    }

    @Test
    @DisplayName("상태와 과거 선택이 다른 두 장면은 각각 다른 게임 데이터를 LLM에 전달한다")
    void differentScenes_sendDifferentContext() {
        reply.set(new Reply(200, completion(NARRATIVE, "k-exaone_v2.5"), 0));
        LlmNarrativeGenerator generator = generator(Duration.ofSeconds(5));

        generator.generate(scene("E10", 1, "STUDY"));
        generator.generate(scene("E20", 2, "PROJECT"));

        assertThat(requestBodies).hasSize(2);
        String first = userContent(requestBodies.get(0));
        String second = userContent(requestBodies.get(1));
        assertThat(first).contains("E10", "\"year\":1", "STUDY_PREV").doesNotContain("E20");
        assertThat(second).contains("E20", "\"year\":2", "PROJECT_PREV", "이전 장면 서사").doesNotContain("E10");
    }

    @Test
    @DisplayName("401 인증 오류는 키를 가린 생성 실패로 바뀐다")
    void unauthorized_failsWithMaskedKey() {
        reply.set(new Reply(401, "{\"error\":{\"message\":\"invalid key " + API_KEY + "\",\"type\":\"auth\"}}", 0));

        assertThatThrownBy(() -> generator(Duration.ofSeconds(5)).generate(scene("AD01", 1, "GENERAL")))
                .isInstanceOf(NarrativeGenerationException.class)
                .hasMessageNotContaining(API_KEY);
    }

    @Test
    @DisplayName("JSON이 아닌 응답은 생성 실패로 처리한다")
    void malformedJson_fails() {
        reply.set(new Reply(200, "<html>bad gateway</html>", 0));

        assertThatThrownBy(() -> generator(Duration.ofSeconds(5)).generate(scene("AD01", 1, "GENERAL")))
                .isInstanceOf(NarrativeGenerationException.class);
    }

    @Test
    @DisplayName("제한 시간을 넘긴 응답은 생성 실패로 처리한다")
    void timeout_fails() {
        reply.set(new Reply(200, completion(NARRATIVE, "k-exaone_v2.5"), 3000));
        LlmNarrativeGenerator generator = generator(Duration.ofMillis(500));
        long started = System.nanoTime();

        assertThatThrownBy(() -> generator.generate(scene("AD01", 1, "GENERAL")))
                .isInstanceOf(NarrativeGenerationException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(2500));
    }

    @Test
    @DisplayName("너무 짧거나 너무 긴 서사 본문은 생성 실패로 처리한다")
    void invalidLength_fails() {
        reply.set(new Reply(200, completion("짧다", "k-exaone_v2.5"), 0));
        assertThatThrownBy(() -> generator(Duration.ofSeconds(5)).generate(scene("AD01", 1, "GENERAL")))
                .isInstanceOf(NarrativeGenerationException.class);

        reply.set(new Reply(200, completion("가".repeat(LlmNarrativeGenerator.MAX_LENGTH + 1), "k-exaone_v2.5"), 0));
        assertThatThrownBy(() -> generator(Duration.ofSeconds(5)).generate(scene("AD01", 1, "GENERAL")))
                .isInstanceOf(NarrativeGenerationException.class);
    }

    private LlmNarrativeGenerator generator(Duration timeout) {
        return (LlmNarrativeGenerator) new NarrativeConfig().narrativeGenerator(properties(API_KEY, timeout), json);
    }

    private NarrativeLlmProperties properties(String apiKey, Duration timeout) {
        return new NarrativeLlmProperties(apiKey, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                "k-exaone_v2.5", timeout, 700, 0.8);
    }

    private static NarrativeScene scene(String sceneId, int year, String choice) {
        NarrativeContext context = new NarrativeContext("우주", "SCHOOL", year, 5, null,
                Map.of("P", 20 + year), "GENERAL", "SW", null, "BACKEND",
                List.of(), List.of(), List.of(new NarrativeContext.RecentChoice(year, 4, null, "E00",
                choice + "_PREV", "DONE")));
        List<String> previous = year == 1 ? List.of() : List.of("이전 장면 서사");
        return new NarrativeScene(sceneId, "장면 " + sceneId, "상황", choice, choice + " 선택", "DONE", "{}",
                year, 5, null, context, previous);
    }

    private String completion(String content, String model) {
        return json.writeValueAsString(Map.of(
                "id", "chatcmpl-1",
                "object", "chat.completion",
                "created", 1763520000,
                "model", model,
                "choices", List.of(Map.of(
                        "index", 0,
                        "finish_reason", "stop",
                        "message", Map.of("role", "assistant", "content", content))),
                "usage", Map.of("prompt_tokens", 10, "completion_tokens", 20, "total_tokens", 30)));
    }

    private String userContent(String requestBody) {
        for (var message : json.readTree(requestBody).path("messages")) {
            if ("user".equals(message.path("role").asString())) {
                return message.path("content").asString();
            }
        }
        throw new AssertionError("user message missing: " + requestBody);
    }
}
