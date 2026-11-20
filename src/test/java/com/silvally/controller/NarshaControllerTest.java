package com.silvally.controller;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.request.NarshaActionRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.NarshaView;
import com.silvally.service.ActivityService;
import com.silvally.service.AdmissionResultService;
import com.silvally.service.InterviewService;
import com.silvally.service.NarshaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({NarshaController.class, EventController.class})
class NarshaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NarshaService narshaService;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private InterviewService interviewService;

    @MockitoBean
    private AdmissionResultService admissionResultService;

    @Test
    @DisplayName("나르샤 조회는 주제 후보와 직접 입력 규칙, 학년별 기록을 반환한다")
    void get_returnsTopicsAndProjects() throws Exception {
        UUID id = UUID.randomUUID();
        given(narshaService.getNarsha(id)).willReturn(new NarshaView(
                List.of(new NarshaView.Topic("T1", "주제", "요약", List.of("기능"), List.of("BACKEND"))),
                List.of(new NarshaView.Role("BACKEND", "서버")), Map.of("minLength", 2, "maxLength", 80),
                List.of(Map.of("year", 1)), null, List.of(), "가상 규칙"));

        mockMvc.perform(get("/api/v1/players/{id}/narsha", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topics[0].code").value("T1"))
                .andExpect(jsonPath("$.data.customTopicRule.maxLength").value(80))
                .andExpect(jsonPath("$.data.projects[0].year").value(1));
    }

    @Test
    @DisplayName("나르샤 행동 요청은 버전·멱등키 헤더와 종류를 서비스에 전달한다")
    void act_passesHeadersAndKind() throws Exception {
        UUID id = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        given(narshaService.act(any(), any(), anyLong(), anyString())).willReturn(
                new MutationResult(null, "NARSHA_ACTION", 1, Map.of(), "WORKED", Map.of()));

        mockMvc.perform(post("/api/v1/players/{id}/narsha/actions", id)
                        .header("X-Expected-Version", 7)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"MENTORING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCode").value("WORKED"));

        ArgumentCaptor<NarshaActionRequest> captor = ArgumentCaptor.forClass(NarshaActionRequest.class);
        verify(narshaService).act(eq(id), captor.capture(), eq(7L), eq(key));
        assertThat(captor.getValue().kind()).isEqualTo("MENTORING");
    }

    @Test
    @DisplayName("이벤트 선택 본문의 narsha 입력은 주제·역할로 읽히고 선택 코드 없이는 거절한다")
    void chooseEvent_readsNarshaInput() throws Exception {
        UUID id = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        given(activityService.chooseEvent(any(), anyString(), anyLong(), anyString(), any())).willReturn(
                new MutationResult(null, "NARSHA_START", 0, Map.of(), "COMPLETED", Map.of()));

        mockMvc.perform(post("/api/v1/players/{id}/events/NARSHA_START/choices", id)
                        .header("X-Expected-Version", 1)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceCode\":\"JOIN\",\"narsha\":{\"topicCode\":\"CUSTOM\","
                                + "\"customTopic\":\"우리 팀 주제\",\"role\":\"BACKEND\"}}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ChooseEventRequest> captor = ArgumentCaptor.forClass(ChooseEventRequest.class);
        verify(activityService).chooseEvent(eq(id), eq("NARSHA_START"), eq(1L), eq(key), captor.capture());
        assertThat(captor.getValue().narsha().customTopic()).isEqualTo("우리 팀 주제");
        assertThat(captor.getValue().narsha().role()).isEqualTo("BACKEND");

        mockMvc.perform(post("/api/v1/players/{id}/events/NARSHA_START/choices", id)
                        .header("X-Expected-Version", 1)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"narsha\":{\"topicCode\":\"CUSTOM\",\"customTopic\":\"선택 코드 없음\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
