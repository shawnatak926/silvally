package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.CatalogEntry;
import com.silvally.entity.CatalogKind;
import com.silvally.service.ActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final ActivityService activityService;

    public CatalogController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/{kind}")
    public ApiResponse<List<CatalogEntry>> catalog(@PathVariable String kind, @RequestParam UUID playerId) {
        CatalogKind catalogKind = CatalogKind.fromPath(kind)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "알 수 없는 카탈로그 종류입니다: " + kind));
        return ApiResponse.ok(activityService.getCatalog(playerId, catalogKind.name()));
    }
}
