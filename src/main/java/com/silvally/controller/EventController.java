package com.silvally.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.silvally.common.ApiResponse;
import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.dto.response.EventView;
import com.silvally.dto.response.MutationResult;
import com.silvally.service.ActivityService;
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

@Tag(name = "이벤트")
@RestController
@RequestMapping("/api/v1/players/{playerId}/events")
public class EventController {
    private final ActivityService activityService;

    public EventController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Operation(summary = "이벤트 목록 조회")
    @GetMapping
    public ApiResponse<List<EventView>> events(@PathVariable UUID playerId) {
        return ApiResponse.ok(activityService.getEvents(playerId));
    }

    @Operation(summary = "이벤트 상세 조회")
    @GetMapping("/{eventCode}")
    public ApiResponse<EventView> event(@PathVariable UUID playerId, @PathVariable String eventCode) {
        return ApiResponse.ok(activityService.getEvent(playerId, eventCode));
    }

    @Operation(summary = "이벤트 선택지 선택")
    @PostMapping("/{eventCode}/choices")
    public ApiResponse<MutationResult> choose(
            @PathVariable UUID playerId,
            @PathVariable String eventCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ChooseEventRequest request) {
        return ApiResponse.ok(activityService.chooseEvent(playerId, eventCode, expectedVersion, idempotencyKey, request));
    }
}
