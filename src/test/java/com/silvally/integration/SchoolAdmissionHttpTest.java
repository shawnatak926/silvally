package com.silvally.integration;

import com.silvally.repository.*;
import com.silvally.support.AdmissionAnswers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** 실제 임의 포트의 HTTP 서버와 Controller–Service–JPA 전체 경로를 사용합니다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchoolAdmissionHttpTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired ActivityRecordRepository records;
    @Autowired InterviewQuestionRepository questions;
    @Autowired CommandLogRepository history;
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    record Response(int status, JsonNode body) { JsonNode data() { return body.path("data"); } }
    record Run(String id, long version, JsonNode question) { }
    @AfterAll void closeClient() { client.close(); }

    @Test void completeAdmissionOverHttpReturnsPersistedEvaluationAndStartsBeforeSchool() throws Exception {
        Run run = prepare(); String key = UUID.randomUUID().toString();
        Map<String,Object> answer = answer(run, AdmissionAnswers.STRONG);
        JsonNode result = ok(submit(run, answer, key, run.version()), 200);
        assertThat(result.path("resultCode").asString()).isEqualTo("PASSED");
        assertThat(result.path("player").path("phase").asString()).isEqualTo("BEFORE_SCHOOL");
        assertThat(result.path("player").path("beforeSchoolDay").asInt()).isEqualTo(1);
        assertThat(result.path("player").path("version").asLong()).isEqualTo(run.version() + 1);
        JsonNode snapshot = ok(call("GET", "/players/" + run.id() + "/events/AD05/result", null, null, null), 200);
        assertThat(snapshot).isEqualTo(result.path("details").path("admissionResult"));
        assertThat(snapshot.path("decision").path("capacity").asInt()).isEqualTo(72);
        assertThat(snapshot.path("interview").path("score").asInt()).isEqualTo(100);
        assertThat(snapshot.path("interview").path("criteria").path("problemSolving").path("evidence").isEmpty()).isFalse();
        assertThat(ok(call("GET", "/players/" + run.id() + "/events", null, null, null), 200).get(0).path("code").asString()).isEqualTo("BS01");
        assertThat(ok(submit(run, answer, key, run.version()), 200)).isEqualTo(result);
        error(submit(run, answer(run, "다른 답변"), key, run.version()), 409, "IDEMPOTENCY_CONFLICT");
        error(submit(run, answer, UUID.randomUUID().toString(), run.version() + 1), 409, "WRONG_PHASE");
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(UUID.fromString(run.id()), "AD05")).hasSize(1);
        assertThat(history.findByPlayerIdAndSubjectCodeOrderByIdAsc(UUID.fromString(run.id()), "AD05")).hasSize(1);
        assertThat(ok(call("GET", "/players/" + run.id() + "/events/AD05/result", null, null, null), 200)).isEqualTo(snapshot);
        Path report = Path.of("build/reports/issue8/successful-flow.json"); Files.createDirectories(report.getParent());
        Files.writeString(report, json.writeValueAsString(Map.of("question", run.question(), "request", answer,
                "submission", result, "savedResult", snapshot)));
    }
    @Test void invalidQuestionAnswerAndVersionLeavePendingInterviewUntouched() throws Exception {
        Run run = prepare(), other = prepare();
        List<Map<String,Object>> invalid = new ArrayList<>();
        for (Object question : Arrays.asList(UUID.randomUUID().toString(), other.question().path("questionId").asString(), "invalid-uuid", null)) {
            var body = new LinkedHashMap<>(answer(run, AdmissionAnswers.STRONG)); body.put("questionId", question); invalid.add(body);
        }
        for (String text : Arrays.asList("", " \n\t ", "x".repeat(1201), null)) {
            var body = new LinkedHashMap<>(answer(run, AdmissionAnswers.STRONG)); body.put("answer", text); invalid.add(body);
        }
        for (var body : invalid) error(submit(run, body, UUID.randomUUID().toString(), run.version()), 400, "VALIDATION_ERROR");
        error(submit(run, answer(run, AdmissionAnswers.STRONG), UUID.randomUUID().toString(), run.version() - 1), 409, "VERSION_CONFLICT");
        assertThat(ok(call("GET", "/players/" + run.id(), null, null, null), 200).path("version").asLong()).isEqualTo(run.version());
        assertThat(questions.findById(UUID.fromString(run.question().path("questionId").asString())).orElseThrow().isCompleted()).isFalse();
        assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(UUID.fromString(run.id()), "AD05")).isEmpty();
        error(call("GET", "/players/" + run.id() + "/events/AD05/result", null, null, null), 409, "WRONG_STEP");
        assertThat(ok(submit(run, answer(run, AdmissionAnswers.STRONG), UUID.randomUUID().toString(), run.version()), 200)
                .path("resultCode").asString()).isEqualTo("PASSED");
    }
    @Test void failureIsPersistedAndWrongAdmissionStageCannotSubmit() throws Exception {
        Run run = prepare();
        JsonNode statsBefore = ok(call("GET", "/players/" + run.id(), null, null, null), 200).path("stats");
        Map<String,Object> forged = new LinkedHashMap<>(answer(run, "모르겠습니다."));
        forged.put("interviewScore", 100); forged.put("passed", true); forged.put("statDelta", Map.of("P", 100));
        JsonNode result = ok(submit(run, forged, UUID.randomUUID().toString(), run.version()), 200);
        assertThat(result.path("player").path("stats")).isEqualTo(statsBefore);
        assertThat(result.path("resultCode").asString()).isEqualTo("FAILED");
        assertThat(result.path("player").path("phase").asString()).isEqualTo("ENDED");
        var saved = ok(call("GET", "/players/" + run.id() + "/events/AD05/result", null, null, null), 200);
        assertThat(saved.path("interview").path("score").asInt()).isZero();
        assertThat(saved.path("decision").path("rank").asInt()).isGreaterThan(72);
        assertThat(saved.path("reasonCodes").toString()).contains("GENERAL_RANK_EXCEEDS_CAPACITY");
        JsonNode created = create(); String id = created.path("id").asString();
        error(call("POST", "/players/" + id + "/events/AD05/choices", answer(run, AdmissionAnswers.STRONG),
                created.path("version").asLong(), UUID.randomUUID().toString()), 409, "WRONG_STEP");
        assertThat(ok(call("GET", "/players/" + id, null, null, null), 200).path("version").asLong()).isZero();
    }
    @Test void validBoundaryLengthsAreAcceptedAndQualityIsEvaluatedSeparately() throws Exception {
        for (String text : List.of("가", "가".repeat(1200))) {
            Run run = prepare();
            JsonNode result = ok(submit(run, answer(run, text), UUID.randomUUID().toString(), run.version()), 200);
            assertThat(result.path("details").path("interview").path("score").asInt()).isZero();
        }
    }
    @Test void concurrentSameKeyReturnsOneSavedAnswerAndDifferentKeysConflictOnVersion() throws Exception {
        for (boolean sameKey : List.of(true, false)) {
            Run run = prepare(); String firstKey = UUID.randomUUID().toString();
            String secondKey = sameKey ? firstKey : UUID.randomUUID().toString();
            var start = new CountDownLatch(1);
            try (var pool = Executors.newFixedThreadPool(2)) {
                var one = pool.submit(() -> { start.await(); return submit(run, answer(run, AdmissionAnswers.STRONG), firstKey, run.version()); });
                var two = pool.submit(() -> { start.await(); return submit(run, answer(run, AdmissionAnswers.STRONG), secondKey, run.version()); });
                start.countDown(); Response a = one.get(20, TimeUnit.SECONDS), b = two.get(20, TimeUnit.SECONDS);
                if (sameKey) { assertThat(a.status()).isEqualTo(200); assertThat(b.status()).isEqualTo(200); assertThat(a.body()).isEqualTo(b.body()); }
                else {
                    assertThat(List.of(a.status(), b.status())).containsExactlyInAnyOrder(200, 409);
                    assertThat((a.status() == 409 ? a : b).body().path("error").path("code").asString()).isEqualTo("VERSION_CONFLICT");
                }
            }
            assertThat(records.findByPlayerIdAndEventCodeOrderByIdAsc(UUID.fromString(run.id()), "AD05")).hasSize(1);
            assertThat(ok(call("GET", "/players/" + run.id(), null, null, null), 200).path("version").asLong()).isEqualTo(run.version() + 1);
        }
    }
    Run prepare() throws Exception {
        JsonNode created = create(); String id = created.path("id").asString(); long version = created.path("version").asLong();
        for (String event : List.of("AD01", "AD02", "AD03", "AD04")) {
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("choiceCode", event.equals("AD01") ? "GENERAL" : event.equals("AD02") ? "BALANCED" : "PREPARED");
            if (event.equals("AD02")) body.put("motivation", "카페 재고 관리 불편을 해결하는 백엔드를 배우고 싶습니다.");
            JsonNode mutation = ok(call("POST", "/players/" + id + "/events/" + event + "/choices", body,
                    version, UUID.randomUUID().toString()), 200);
            assertThat(mutation.path("player").path("version").asLong()).isEqualTo(version + 1);
            version = mutation.path("player").path("version").asLong();
        }
        JsonNode question = ok(call("GET", "/players/" + id + "/events/AD05/interview", null, null, null), 200);
        assertThat(question.path("kind").asString()).isEqualTo("SCHOOL");
        assertThat(question.path("question").asString()).contains("카페 재고", "AD03");
        assertThat(ok(call("GET", "/players/" + id + "/events/AD05/interview", null, null, null), 200)).isEqualTo(question);
        return new Run(id, version, question);
    }
    JsonNode create() throws Exception {
        return ok(call("POST", "/players", Map.of("name", "HTTP 지원자"), null, UUID.randomUUID().toString()), 201);
    }
    Map<String,Object> answer(Run run, String text) {
        return Map.of("choiceCode", "ANSWER", "questionId", run.question().path("questionId").asString(), "answer", text);
    }
    Response submit(Run run, Map<String,Object> answer, String key, long version) throws Exception {
        return call("POST", "/players/" + run.id() + "/events/AD05/choices", answer, version, key);
    }
    Response call(String method, String path, Object body, Long version, String key) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1" + path)).timeout(Duration.ofSeconds(10));
        if (version != null) builder.header("X-Expected-Version", version.toString());
        if (key != null) builder.header("Idempotency-Key", key);
        if (body == null) builder.GET();
        else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), json.readTree(response.body()));
    }
    JsonNode ok(Response response, int expected) {
        assertThat(response.status()).describedAs(response.body().toString()).isEqualTo(expected);
        assertThat(response.body().path("success").asBoolean()).isTrue(); return response.data();
    }
    void error(Response response, int status, String code) {
        assertThat(response.status()).describedAs(response.body().toString()).isEqualTo(status);
        assertThat(response.body().path("success").asBoolean()).isFalse();
        assertThat(response.body().path("error").path("code").asString()).isEqualTo(code);
    }
}
