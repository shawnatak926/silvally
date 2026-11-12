package com.silvally.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.silvally.common.ApiResponse;
import com.silvally.dto.response.ItemStack;
import com.silvally.dto.response.MutationResult;
import com.silvally.service.ActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "인벤토리")
@RestController
@RequestMapping("/api/v1/players/{playerId}/inventory")
public class InventoryController {

    private final ActivityService activityService;

    public InventoryController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Operation(summary = "인벤토리 조회")
    @GetMapping
    public ApiResponse<List<ItemStack>> inventory(@PathVariable UUID playerId) {
        return ApiResponse.ok(activityService.getInventory(playerId));
    }

    @Operation(summary = "아이템 사용")
    @PostMapping("/{itemCode}/use")
    public ApiResponse<MutationResult> use(
            @PathVariable UUID playerId,
            @PathVariable String itemCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(activityService.useItem(playerId, itemCode, expectedVersion, idempotencyKey));
    }
}
