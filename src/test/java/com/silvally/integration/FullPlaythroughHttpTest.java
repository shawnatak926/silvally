package com.silvally.integration;

import com.silvally.support.AdmissionAnswers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 프론트 자동 진행과 같은 순서로 실제 HTTP 서버에서 회차 생성부터 엔딩까지 진행한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullPlaythroughHttpTest {

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    record Response(int status, JsonNode body) {
        JsonNode data() { return body.path("data"); }
    }

    @AfterAll
    void closeClient() {
        client.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOMESTIC", "SILICON_VALLEY"})
    void admissionToGraduationFollowsReadinessAndStaysConsistentAfterReload(String fieldPath) throws Exception {
        JsonNode player = ok(call("POST", "/players", Map.of("name", "전체 진행"), UUID.randomUUID().toString()), 201);
        String id = player.path("id").asString();
        assertThat(player.path("admissionEligibility").path("GENERAL").asBoolean()).isTrue();

        player = admission(id, player);
        assertThat(player.path("phase").asString()).isEqualTo("BEFORE_SCHOOL");
        player = beforeSchool(id, player);
        assertThat(player.path("phase").asString()).isEqualTo("CAREER_PICK");

        JsonNode preference = ok(call("GET", "/players/" + id + "/career-preference", null, null), 200);
        assertThat(preference.path("recommendations")).hasSize(3);
        String role = preference.path("recommendations").get(0).path("roleCode").asString();
        player = mutate("PUT", "/players/" + id + "/career-preference", Map.of("roleCode", role), player).path("player");
        assertThat(player.path("phase").asString()).isEqualTo("SCHOOL");

        Counts counts = school(id, player, fieldPath);
        assertThat(counts.regularTurns).isEqualTo(72);
        assertThat(counts.practiceTurns).isEqualTo(2);
        assertThat(counts.yearChoices).containsExactly("MAJOR", "TRACK");

        JsonNode beforeGraduation = ok(call("GET", "/players/" + id, null, null), 200);
        assertThat(beforeGraduation.path("phase").asString()).isEqualTo("GRAD_READY");
        JsonNode graduation = mutate("POST", "/players/" + id + "/graduation", Map.of("path", "AUTO"), beforeGraduation);
        assertThat(graduation.path("player").path("phase").asString()).isEqualTo("ENDED");

        JsonNode ending = ok(call("GET", "/players/" + id + "/ending", null, null), 200);
        assertThat(ending.path("endingCode").asString()).isNotBlank();
        assertThat(ok(call("GET", "/players/" + id + "/ending", null, null), 200)).isEqualTo(ending);
        assertThat(ok(call("GET", "/players/" + id + "/field-learning", null, null), 200).path("path").asString())
                .isEqualTo(fieldPath);

        JsonNode reloaded = ok(call("GET", "/players/" + id, null, null), 200);
        assertThat(reloaded).isEqualTo(graduation.path("player"));
        JsonNode progress = ok(call("GET", "/players/" + id + "/progress", null, null), 200);
        assertThat(progress.path("player")).isEqualTo(reloaded);
        assertThat(progress.path("yearSummaries")).hasSize(3)
                .allSatisfy(year -> assertThat(year.path("exams")).hasSize(6));
        assertThat(progress.path("yearSummaries").get(2).path("gradeAfter").asInt())
                .isEqualTo(reloaded.path("stats").path("G").asInt());

        List<Long> firstRead = historyIds(id);
        assertThat(firstRead).isNotEmpty().doesNotHaveDuplicates().isSorted();
        assertThat(historyIds(id)).isEqualTo(firstRead);
    }

    private JsonNode admission(String id, JsonNode player) throws Exception {
        for (String event : List.of("AD01", "AD02", "AD03", "AD04")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("choiceCode", event.equals("AD01") ? "GENERAL" : event.equals("AD02") ? "BALANCED" : "PREPARED");
            if (event.equals("AD02")) body.put("motivation", "카페 재고 관리 불편을 해결하는 백엔드를 배우고 싶습니다.");
            player = mutate("POST", "/players/" + id + "/events/" + event + "/choices", body, player).path("player");
        }
        JsonNode question = ok(call("GET", "/players/" + id + "/events/AD05/interview", null, null), 200);
        JsonNode result = mutate("POST", "/players/" + id + "/events/AD05/choices", Map.of("choiceCode", "ANSWER",
                "questionId", question.path("questionId").asString(), "answer", AdmissionAnswers.STRONG), player);
        assertThat(result.path("resultCode").asString()).isEqualTo("PASSED");
        return result.path("player");
    }

    private JsonNode beforeSchool(String id, JsonNode player) throws Exception {
        for (int day = 1; day <= 5; day++) {
            String event = "BS%02d".formatted(day);
            List<String> enabled = enabledChoices(id, event);
            Map<String, Object> body = day <= 2
                    ? Map.of("choiceCodes", enabled.subList(0, 2))
                    : Map.of("choiceCode", enabled.getFirst());
            player = mutate("POST", "/players/" + id + "/events/" + event + "/choices", body, player).path("player");
            assertThat(player.path("version").asLong()).isPositive();
        }
        return player;
    }

    private static final class Counts {
        int regularTurns;
        int practiceTurns;
        final List<String> yearChoices = new ArrayList<>();
    }

    private Counts school(String id, JsonNode player, String fieldPath) throws Exception {
        Counts counts = new Counts();
        boolean campTaken = false;
        for (int guard = 0; guard < 400; guard++) {
            JsonNode readiness = ok(call("GET", "/players/" + id + "/turns/readiness", null, null), 200);
            assertThat(readiness.path("version").asLong()).isEqualTo(player.path("version").asLong());
            if (readiness.path("canAdvance").asBoolean()) {
                boolean practice = player.path("school").path("practiceRemaining").asInt() > 0;
                JsonNode turn = mutate("POST", "/players/" + id + "/turns/advance", null, player);
                if (practice) counts.practiceTurns++; else counts.regularTurns++;
                player = turn.path("player");
                continue;
            }
            JsonNode blocker = readiness.path("blockers").get(0);
            String type = blocker.path("type").asString(), code = blocker.path("code").asString();
            if (type.equals("PHASE")) {
                assertThat(code).isEqualTo("GRAD_READY");
                return counts;
            }
            if (type.equals("YEAR_CHOICE")) {
                counts.yearChoices.add(code);
                player = mutate("POST", "/players/" + id + "/year-transition",
                        Map.of("choiceCode", blocker.path("options").get(0).asString()), player).path("player");
                assertThat(player.path("school").path("pendingChoice").isNull()).isTrue();
                continue;
            }
            assertThat(type).isEqualTo("REQUIRED_EVENT");
            String choice;
            if (code.startsWith("VACATION_")) {
                choice = campTaken ? "REST" : "CAMP";
                campTaken = true;
            } else if (code.equals("J01")) {
                choice = fieldPath;
            } else {
                choice = enabledChoices(id, code).getFirst();
            }
            player = mutate("POST", "/players/" + id + "/events/" + code + "/choices", Map.of("choiceCode", choice), player)
                    .path("player");
            if (choice.equals("CAMP")) assertThat(player.path("school").path("practiceRemaining").asInt()).isEqualTo(2);
        }
        throw new AssertionError("졸업 준비 단계에 도달하지 못했습니다.");
    }

    private List<String> enabledChoices(String id, String event) throws Exception {
        List<String> codes = new ArrayList<>();
        ok(call("GET", "/players/" + id + "/events/" + event, null, null), 200).path("choices").forEach(choice -> {
            if (choice.path("enabled").asBoolean()) codes.add(choice.path("code").asString());
        });
        assertThat(codes).describedAs(event).isNotEmpty();
        return codes;
    }

    private List<Long> historyIds(String id) throws Exception {
        List<Long> ids = new ArrayList<>();
        long after = 0;
        while (true) {
            JsonNode page = ok(call("GET", "/players/" + id + "/history?limit=50&afterId=" + after, null, null), 200);
            page.path("items").forEach(item -> ids.add(item.path("logId").asLong()));
            if (page.path("nextAfterId").isNull() || page.path("nextAfterId").isMissingNode()) return ids;
            after = page.path("nextAfterId").asLong();
        }
    }

    private JsonNode mutate(String method, String path, Object body, JsonNode player) throws Exception {
        long version = player.path("version").asLong();
        String key = UUID.randomUUID().toString();
        Response first = call(method, path, body, version, key);
        JsonNode result = ok(first, 200);
        assertThat(result.path("player").path("version").asLong()).isGreaterThan(version);
        assertThat(ok(call(method, path, body, version, key), 200)).isEqualTo(result);
        return result;
    }

    private Response call(String method, String path, Object body, String key) throws Exception {
        return call(method, path, body, null, key);
    }

    private Response call(String method, String path, Object body, Long version, String key) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1" + path))
                .timeout(Duration.ofSeconds(10));
        if (version != null) builder.header("X-Expected-Version", version.toString());
        if (key != null) builder.header("Idempotency-Key", key);
        if (method.equals("GET")) builder.GET();
        else builder.header("Content-Type", "application/json").method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), json.readTree(response.body()));
    }

    private JsonNode ok(Response response, int expected) {
        assertThat(response.status()).describedAs(response.body().toString()).isEqualTo(expected);
        assertThat(response.body().path("success").asBoolean()).isTrue();
        return response.data();
    }
}
