package com.silvally.integration;

import com.silvally.entity.Player;
import com.silvally.repository.PlayerRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SchoolLifeHttpTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired PlayerRepository players;
    @Autowired PlatformTransactionManager transactions;
    final HttpClient client = HttpClient.newHttpClient();
    @AfterEach void close() { client.close(); }
    record Response(int status, JsonNode data) { }
    Response call(String method, String path, Object body, Long version, String key) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
        if (version != null) builder.header("X-Expected-Version", version.toString());
        if (key != null) builder.header("Idempotency-Key", key);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), json.readTree(response.body()));
    }
    @Test void publicCatalogProvides35CitedSourcesWithPlansAndConflictsSeparated() throws Exception {
        var response = call("GET", "/catalog/school-life", null, null, null);
        assertThat(response.status()).isEqualTo(200); var sources = response.data().path("data");
        assertThat(sources.size()).isEqualTo(35);
        var byCode = new HashMap<String, JsonNode>(); sources.forEach(s -> byCode.put(s.path("code").asString(), s));
        assertThat(byCode.get("REBUILD_PLAN").path("classification").asString()).isEqualTo("PLAN");
        assertThat(byCode.get("GLOBAL_PERIOD_NOTE").path("classification").asString()).isEqualTo("CONFLICT");
        assertThat(byCode.get("NEUROW").path("url").asString()).endsWith("#page=22");
        assertThat(byCode.get("GLOBAL_SELECTION").path("fact").asString()).contains("6주", "10명");
        assertThat(byCode.get("GLOBAL_PERIOD_NOTE").path("fact").asString()).contains("8주", "6주");
    }
    @Test void concurrentReadsKeepSameDrawAndChoiceAndRetryAndHistoryWorkThroughHttp() throws Exception {
        var created = call("POST", "/players", Map.of("name", "HTTP 학생"), null, UUID.randomUUID().toString());
        assertThat(created.status()).isEqualTo(201);
        UUID id = UUID.fromString(created.data().path("data").path("id").asString());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Player p = players.findById(id).orElseThrow(); p.startSchool(3); p.chooseSemesterFocus("C"); p.getStats().apply(Map.of("S", 72));
        });
        long version = players.findById(id).orElseThrow().getVersion(); String path = "/players/" + id;
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> call("GET", path + "/events", null, null, null));
            var b = pool.submit(() -> call("GET", path + "/events", null, null, null));
            assertThat(a.get().data()).isEqualTo(b.get().data());
            assertThat(a.get().data().path("data").get(0).path("blocksAuto").asBoolean()).isTrue();
        }
        assertThat(players.findById(id).orElseThrow().getVersion()).isEqualTo(version);
        assertThat(call("POST", path + "/turns/advance", null, version, UUID.randomUUID().toString()).status()).isEqualTo(409);
        assertThat(call("POST", path + "/events/SCHOOL_DOUBTS/choices", Map.of("choiceCode", "CONFIRM_WITHDRAWAL"), version, UUID.randomUUID().toString()).status()).isEqualTo(400);
        String key = UUID.randomUUID().toString(); var request = Map.of("choiceCode", "CONSIDER_WITHDRAWAL");
        var first = call("POST", path + "/events/SCHOOL_DOUBTS/choices", request, version, key);
        assertThat(first.status()).isEqualTo(200);
        assertThat(call("POST", path + "/events/SCHOOL_DOUBTS/choices", request, version, key).data()).isEqualTo(first.data());
        version = first.data().path("data").path("player").path("version").asLong();
        var end = call("POST", path + "/events/WITHDRAWAL_DECISION/choices", Map.of("choiceCode", "CONFIRM_WITHDRAWAL"), version, UUID.randomUUID().toString());
        assertThat(end.status()).isEqualTo(200);
        assertThat(call("GET", path + "/ending", null, null, null).data().path("data").path("endingCode").asString()).isEqualTo("SCHOOL_WITHDRAWAL");
        var history = call("GET", path + "/history?limit=50", null, null, null);
        assertThat(history.data().toString()).contains("WITHDRAWAL_CONSIDERED", "SCHOOL_WITHDRAWAL", "evidenceJson", "stressLoad");
    }
}
