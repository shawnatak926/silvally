package com.silvally.controller;

import com.silvally.dto.response.PlayerView;
import com.silvally.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @Test
    @DisplayName("회차 조회는 200과 통일된 JSON 응답을 반환한다")
    void getPlayer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(playerService.get(id)).willReturn(new PlayerView(
                id, "tester", "v1.1", "ADMISSION_1", 0, 0, 0, 0L,
                Map.of("P", 20), "OPEN", null, null, null, null, null, null, 0));

        mockMvc.perform(get("/api/v1/players/{playerId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("tester"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("이름이 비어 있으면 400 VALIDATION_ERROR를 반환한다")
    void createPlayer_withBlankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
