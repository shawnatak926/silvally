package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.request.PerformActionRequest;
import com.silvally.dto.response.AvailableAction;
import com.silvally.dto.response.MutationResult;
import com.silvally.service.ActivityService;
import com.silvally.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/players/{playerId}")
public class ActionController {
    private final ActivityService activityService;
    private final PlayerService playerService;

    public ActionController(ActivityService activityService, PlayerService playerService) {
        this.activityService = activityService;
        this.playerService = playerService;
    }

    @GetMapping("/actions")
    public ApiResponse<List<AvailableAction>> actions(@PathVariable UUID playerId) {
        return ApiResponse.ok(activityService.getActions(playerId));
    }

    @PostMapping("/actions")
    public ApiResponse<MutationResult> perform(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PerformActionRequest request) {
        return ApiResponse.ok(activityService.performAction(playerId, expectedVersion, idempotencyKey, request));
    }

    @PostMapping("/turns/advance")
    public ApiResponse<MutationResult> advanceTurn(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(playerService.advanceTurn(playerId, expectedVersion, idempotencyKey));
    }
}
