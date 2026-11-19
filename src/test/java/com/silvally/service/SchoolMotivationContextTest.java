package com.silvally.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class SchoolMotivationContextTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    final String motivation = "가족 카페의 재고 관리 불편을 줄이는 백엔드 서비스를 만들고 싶어 지원했습니다.";
    @Test void schoolQuestionIncludesSubmittedMotivationAndPriorActivitiesAndRefreshIsStable() throws Exception {
        var created = request(post("/api/v1/players").header("Idempotency-Key", UUID.randomUUID().toString()), Map.of("name", "지원자"), 201);
        String id = created.path("id").asString(); long version = created.path("version").asLong();
        for (String event : List.of("AD01", "AD02", "AD03", "AD04")) {
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("choiceCode", event.equals("AD01") ? "GENERAL" : event.equals("AD02") ? "BALANCED" : "PREPARED");
            if (event.equals("AD02")) body.put("motivation", motivation);
            var result = request(post("/api/v1/players/" + id + "/events/" + event + "/choices")
                    .header("X-Expected-Version", version).header("Idempotency-Key", UUID.randomUUID().toString()), body, 200);
            version = result.path("player").path("version").asLong();
        }
        var q = request(get("/api/v1/players/" + id + "/events/AD05/interview"), null, 200);
        assertThat(q.path("question").asString()).contains("카페", "재고 관리", "AD03");
        assertThat(q.path("kind").asString()).isEqualTo("SCHOOL");
        assertThat(request(get("/api/v1/players/" + id + "/events/AD05/interview"), null, 200)).isEqualTo(q);
        assertThat(request(get("/api/v1/players/" + id), null, 200).path("version").asLong()).isEqualTo(version);
    }
    @Test void invalidOrWrongStageMotivationDoesNotAdvanceTheRun() throws Exception {
        var created = request(post("/api/v1/players").header("Idempotency-Key", UUID.randomUUID().toString()), Map.of("name", "검증"), 201);
        String id = created.path("id").asString();
        for (String text : List.of(motivation, " ", "x".repeat(1201))) {
            mvc.perform(post("/api/v1/players/" + id + "/events/AD01/choices")
                    .header("X-Expected-Version", 0).header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("choiceCode", "GENERAL", "motivation", text))))
                    .andExpect(status().isBadRequest());
        }
        assertThat(request(get("/api/v1/players/" + id), null, 200).path("version").asLong()).isZero();
    }
    JsonNode request(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, Object body, int status) throws Exception {
        if (body != null) builder.contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(builder).andExpect(status().is(status)).andReturn().getResponse().getContentAsString()).path("data");
    }
}
