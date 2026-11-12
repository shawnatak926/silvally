package com.silvally.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.silvally.common.ApiResponse;
import com.silvally.dto.request.SelectCareerPreferenceRequest;
import com.silvally.dto.response.CareerPreferenceMutation;
import com.silvally.dto.response.CareerPreferenceView;
import com.silvally.service.CareerPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "희망 직무")
@RestController
@RequestMapping("/api/v1/players/{playerId}/career-preference")
public class CareerPreferenceController {
    private final CareerPreferenceService careerPreferenceService;

    public CareerPreferenceController(CareerPreferenceService careerPreferenceService) {
        this.careerPreferenceService = careerPreferenceService;
    }

    @Operation(summary = "희망 직무 추천 조회")
    @GetMapping
    public ApiResponse<CareerPreferenceView> get(@PathVariable UUID playerId) {
        return ApiResponse.ok(careerPreferenceService.getPreference(playerId));
    }

    @Operation(summary = "희망 직무 확정·변경")
    @PutMapping
    public ApiResponse<CareerPreferenceMutation> select(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SelectCareerPreferenceRequest request) {
        return ApiResponse.ok(careerPreferenceService.select(playerId, expectedVersion, idempotencyKey, request));
    }
}
