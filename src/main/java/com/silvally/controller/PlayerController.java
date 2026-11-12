package com.silvally.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.silvally.common.ApiResponse;
import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.dto.response.DeleteResult;
import com.silvally.dto.response.HistoryPage;
import com.silvally.dto.response.PlayerView;
import com.silvally.dto.response.ProgressView;
import com.silvally.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Tag(name = "회차")
@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Operation(summary = "회차 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerView>> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePlayerRequest request) {
        PlayerView view = playerService.create(idempotencyKey, request);
        return ResponseEntity.created(URI.create("/api/v1/players/" + view.id()))
                .body(ApiResponse.ok(view));
    }

    @Operation(summary = "회차 조회")
    @GetMapping("/{playerId}")
    public ApiResponse<PlayerView> get(@PathVariable UUID playerId) {
        return ApiResponse.ok(playerService.get(playerId));
    }

    @Operation(summary = "회차 이름 변경")
    @PutMapping("/{playerId}")
    public ApiResponse<PlayerView> rename(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RenamePlayerRequest request) {
        return ApiResponse.ok(playerService.rename(playerId, expectedVersion, idempotencyKey, request));
    }

    @Operation(summary = "회차 삭제")
    @DeleteMapping("/{playerId}")
    public ApiResponse<DeleteResult> delete(
            @PathVariable UUID playerId,
            @RequestHeader("X-Expected-Version") long expectedVersion) {
        return ApiResponse.ok(playerService.delete(playerId, expectedVersion));
    }

    @Operation(summary = "진행 현황 조회")
    @GetMapping("/{playerId}/progress")
    public ApiResponse<ProgressView> progress(@PathVariable UUID playerId) {
        return ApiResponse.ok(playerService.getProgress(playerId));
    }

    @Operation(summary = "이력 조회")
    @GetMapping("/{playerId}/history")
    public ApiResponse<HistoryPage> history(
            @PathVariable UUID playerId,
            @RequestParam(defaultValue = "0") long afterId,
            @RequestParam(defaultValue = "20") int limit) {
        if (afterId < 0 || limit < 1 || limit > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "afterId는 0 이상, limit은 1~50이어야 합니다.");
        }
        return ApiResponse.ok(playerService.getHistory(playerId, afterId, limit));
    }
}
