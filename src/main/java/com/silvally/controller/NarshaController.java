package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.request.NarshaActionRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.NarshaView;
import com.silvally.service.NarshaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "나르샤")
@RestController
@RequestMapping("/api/v1/players/{playerId}/narsha")
public class NarshaController {

    private final NarshaService narshaService;

    public NarshaController(NarshaService narshaService) {
        this.narshaService = narshaService;
    }

    @Operation(summary = "나르샤 주제 후보·입력 규칙과 학년별 프로젝트 기록 조회")
    @GetMapping
    public ApiResponse<NarshaView> get(@PathVariable UUID playerId) {
        return ApiResponse.ok(narshaService.getNarsha(playerId));
    }

    @Operation(summary = "나르샤 작업 또는 멘토링 (AP 1, 한 턴에 한 번)")
    @PostMapping("/actions")
    public ApiResponse<MutationResult> act(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody NarshaActionRequest request) {
        return ApiResponse.ok(narshaService.act(playerId, request, expectedVersion, idempotencyKey));
    }
}
