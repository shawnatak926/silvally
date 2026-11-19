package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.ProjectView;
import com.silvally.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "프로젝트·포트폴리오")
@RestController
@RequestMapping("/api/v1/players/{playerId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "프로젝트 목록과 시작 가능 여부 조회")
    @GetMapping
    public ApiResponse<List<ProjectView>> list(@PathVariable UUID playerId) {
        return ApiResponse.ok(projectService.getProjects(playerId));
    }

    @Operation(summary = "프로젝트 시작")
    @PostMapping("/{projectCode}/start")
    public ApiResponse<MutationResult> start(
            @PathVariable UUID playerId,
            @PathVariable String projectCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(projectService.start(playerId, projectCode, expectedVersion, idempotencyKey));
    }

    @Operation(summary = "프로젝트 작업 (AP 1, 작업 스냅샷 기록)")
    @PostMapping("/{projectCode}/work")
    public ApiResponse<MutationResult> work(
            @PathVariable UUID playerId,
            @PathVariable String projectCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(projectService.work(playerId, projectCode, expectedVersion, idempotencyKey));
    }

    @Operation(summary = "완료 작품 포트폴리오 등록")
    @PostMapping("/{projectCode}/portfolio")
    public ApiResponse<MutationResult> registerPortfolio(
            @PathVariable UUID playerId,
            @PathVariable String projectCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(projectService.registerPortfolio(playerId, projectCode, expectedVersion, idempotencyKey));
    }
}
