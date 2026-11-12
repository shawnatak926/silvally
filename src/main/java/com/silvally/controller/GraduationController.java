package com.silvally.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.silvally.common.ApiResponse;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.response.EndingMutation;
import com.silvally.dto.response.EndingView;
import com.silvally.service.CareerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "졸업·엔딩")
@RestController
@RequestMapping("/api/v1/players/{playerId}")
public class GraduationController {

    private final CareerService careerService;

    public GraduationController(CareerService careerService) {
        this.careerService = careerService;
    }

    @Operation(summary = "졸업 확정")
    @PostMapping("/graduation")
    public ApiResponse<EndingMutation> graduate(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody FinalizeGraduationRequest request) {
        return ApiResponse.ok(careerService.graduate(playerId, expectedVersion, idempotencyKey, request));
    }

    @Operation(summary = "엔딩 조회")
    @GetMapping("/ending")
    public ApiResponse<EndingView> ending(@PathVariable UUID playerId) {
        return ApiResponse.ok(careerService.getEnding(playerId));
    }
}
