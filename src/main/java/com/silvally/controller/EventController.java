package com.silvally.controller;

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

@RestController
@RequestMapping("/api/v1/players/{playerId}/events")
public class EventController {
    private final ActivityService activityService;

    public EventController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<List<EventView>> events(@PathVariable UUID playerId) {
        return ApiResponse.ok(activityService.getEvents(playerId));
    }

    @GetMapping("/{eventCode}")
    public ApiResponse<EventView> event(@PathVariable UUID playerId, @PathVariable String eventCode) {
        return ApiResponse.ok(activityService.getEvent(playerId, eventCode));
    }

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
