package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.request.ApplyJobRequest;
import com.silvally.dto.request.JobStepRequest;
import com.silvally.dto.response.ApplicationMutation;
import com.silvally.dto.response.ApplicationView;
import com.silvally.service.CareerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/players/{playerId}/applications")
public class ApplicationController {
    private final CareerService careerService;

    public ApplicationController(CareerService careerService) {
        this.careerService = careerService;
    }

    @GetMapping
    public ApiResponse<List<ApplicationView>> list(@PathVariable UUID playerId) {
        return ApiResponse.ok(careerService.getApplications(playerId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationMutation>> apply(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ApplyJobRequest request) {
        ApplicationMutation result = careerService.apply(playerId, expectedVersion, idempotencyKey, request);
        return ResponseEntity
                .created(URI.create("/api/v1/players/" + playerId + "/applications/" + result.application().id()))
                .body(ApiResponse.ok(result));
    }

    @PostMapping("/{applicationId}/steps")
    public ApiResponse<ApplicationMutation> step(
            @PathVariable UUID playerId,
            @PathVariable UUID applicationId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody JobStepRequest request) {
        return ApiResponse.ok(careerService.proceedStep(playerId, applicationId, expectedVersion, idempotencyKey, request));
    }
}
