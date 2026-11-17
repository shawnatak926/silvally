package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.request.YearTransitionRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.YearTransitionView;
import com.silvally.service.SchoolTurnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "재학·학년 전환")
@RestController
@RequestMapping("/api/v1/players/{playerId}/year-transition")
public class SchoolController {

    private final SchoolTurnService schoolTurnService;

    public SchoolController(SchoolTurnService schoolTurnService) {
        this.schoolTurnService = schoolTurnService;
    }

    @Operation(summary = "학년 전환 필수 선택 조회")
    @GetMapping
    public ApiResponse<YearTransitionView> get(@PathVariable UUID playerId) {
        return ApiResponse.ok(schoolTurnService.getYearTransition(playerId));
    }

    @Operation(summary = "학년 전환 필수 선택(전공·트랙) 확정")
    @PostMapping
    public ApiResponse<MutationResult> choose(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody YearTransitionRequest request) {
        return ApiResponse.ok(schoolTurnService.chooseYearTransition(playerId, expectedVersion, idempotencyKey, request));
    }
}
