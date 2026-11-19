package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.response.NarrativeRetryResult;
import com.silvally.service.NarrativeRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "인생 기록·서사")
@RestController
@RequestMapping("/api/v1/players/{playerId}/narratives")
public class NarrativeController {

    private final NarrativeRecoveryService recoveryService;

    public NarrativeController(NarrativeRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @Operation(summary = "저장되지 않은(PENDING·FAILED) AI 서사 재시도")
    @PostMapping("/retry")
    public ApiResponse<NarrativeRetryResult> retry(@PathVariable UUID playerId) {
        return ApiResponse.ok(recoveryService.retry(playerId));
    }
}
