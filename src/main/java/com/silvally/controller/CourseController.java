package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.dto.response.CourseView;
import com.silvally.dto.response.MutationResult;
import com.silvally.service.CertificateService;
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

@Tag(name = "방과후·자격증")
@RestController
@RequestMapping("/api/v1/players/{playerId}/courses")
public class CourseController {

    private final CertificateService certificateService;

    public CourseController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Operation(summary = "방과후 과정 목록과 수강 가능 여부 조회")
    @GetMapping
    public ApiResponse<List<CourseView>> list(@PathVariable UUID playerId) {
        return ApiResponse.ok(certificateService.getCourses(playerId));
    }

    @Operation(summary = "방과후 과정 수강 신청")
    @PostMapping("/{courseCode}/enroll")
    public ApiResponse<MutationResult> enroll(
            @PathVariable UUID playerId,
            @PathVariable String courseCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(certificateService.enroll(playerId, courseCode, expectedVersion, idempotencyKey));
    }

    @Operation(summary = "방과후 과정 출석 (AP 1)")
    @PostMapping("/{courseCode}/attend")
    public ApiResponse<MutationResult> attend(
            @PathVariable UUID playerId,
            @PathVariable String courseCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(certificateService.attend(playerId, courseCode, expectedVersion, idempotencyKey));
    }

    @Operation(summary = "자격증 시험 응시 (출석 2회 후, AP 1)")
    @PostMapping("/{courseCode}/exam")
    public ApiResponse<MutationResult> exam(
            @PathVariable UUID playerId,
            @PathVariable String courseCode,
            @RequestHeader("X-Expected-Version") long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.ok(certificateService.takeExam(playerId, courseCode, expectedVersion, idempotencyKey));
    }
}
