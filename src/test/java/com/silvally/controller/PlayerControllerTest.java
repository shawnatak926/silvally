package com.silvally.controller;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.dto.response.DeleteResult;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @DisplayName("회차 생성은 201과 Location 헤더, 생성된 회차를 반환한다")
    void createPlayer_returns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        given(playerService.create(eq(key), any(CreatePlayerRequest.class))).willReturn(view(id, "tester", 0L));

        mockMvc.perform(post("/api/v1/players")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"tester\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/players/" + id))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    @DisplayName("Idempotency-Key 헤더 없이 생성하면 400 VALIDATION_ERROR이고 서비스를 호출하지 않는다")
    void createPlayer_withoutIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"tester\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(playerService, never()).create(anyString(), any());
    }

    @Test
    @DisplayName("이름 변경은 X-Expected-Version과 Idempotency-Key를 서비스로 전달하고 갱신된 버전을 반환한다")
    void renamePlayer_passesHeaders() throws Exception {
        UUID id = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        given(playerService.rename(id, 3L, key, new RenamePlayerRequest("renamed")))
                .willReturn(view(id, "renamed", 4L));

        mockMvc.perform(put("/api/v1/players/{playerId}", id)
                        .header("X-Expected-Version", "3")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("renamed"))
                .andExpect(jsonPath("$.data.version").value(4));
    }

    @Test
    @DisplayName("X-Expected-Version 헤더가 없으면 400 VALIDATION_ERROR")
    void renamePlayer_withoutExpectedVersion_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/players/{playerId}", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("오래된 버전 요청은 409 VERSION_CONFLICT와 현재 버전 정보를 반환한다")
    void renamePlayer_staleVersion_returns409WithDetails() throws Exception {
        UUID id = UUID.randomUUID();
        given(playerService.rename(eq(id), anyLong(), anyString(), any(RenamePlayerRequest.class)))
                .willThrow(new BusinessException(ErrorCode.VERSION_CONFLICT,
                        Map.of("expectedVersion", 1L, "currentVersion", 2L)));

        mockMvc.perform(put("/api/v1/players/{playerId}", id)
                        .header("X-Expected-Version", "1")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.error.details.currentVersion").value(2));
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 다른 내용을 보내면 409 IDEMPOTENCY_CONFLICT")
    void renamePlayer_idempotencyConflict_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        given(playerService.rename(eq(id), anyLong(), anyString(), any(RenamePlayerRequest.class)))
                .willThrow(new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT));

        mockMvc.perform(put("/api/v1/players/{playerId}", id)
                        .header("X-Expected-Version", "0")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"other\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @DisplayName("없는 회차를 조회하면 404 NOT_FOUND")
    void getPlayer_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(playerService.get(id)).willThrow(new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/players/{playerId}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("회차를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("회차 삭제는 X-Expected-Version을 전달하고 삭제 결과를 반환한다")
    void deletePlayer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(playerService.delete(id, 2L)).willReturn(new DeleteResult(id, true));

        mockMvc.perform(delete("/api/v1/players/{playerId}", id)
                        .header("X-Expected-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    private static PlayerView view(UUID id, String name, long version) {
        return new PlayerView(id, name, "v1.1", "ADMISSION_1", 0, 0, 0, version,
                Map.of("P", 20), "OPEN", null, null, null, null, null, null, 0);
    }
}
