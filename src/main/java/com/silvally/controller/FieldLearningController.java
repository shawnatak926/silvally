package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.response.FieldLearningView;
import com.silvally.service.FieldLearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@Tag(name = "현장학습")
@RequestMapping("/api/v1/players/{playerId}/field-learning")
public class FieldLearningController {
    private final FieldLearningService learning;
    public FieldLearningController(FieldLearningService learning) { this.learning = learning; }
    @GetMapping("/summary") @Operation(summary = "현장학습 인생 기록·졸업 요약 연동 데이터")
    public ApiResponse<com.silvally.dto.response.FieldLearningSummaryView> summary(@PathVariable UUID playerId) {
        return ApiResponse.ok(learning.summary(playerId));
    }
    @GetMapping @Operation(summary = "현장학습 신청·선발·참가 상태 조회")
    public ApiResponse<FieldLearningView> get(@PathVariable UUID playerId) { return ApiResponse.ok(learning.get(playerId)); }
}
