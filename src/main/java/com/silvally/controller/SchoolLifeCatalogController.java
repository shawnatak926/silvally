package com.silvally.controller;

import com.silvally.common.ApiResponse;
import com.silvally.service.SchoolLifeContent;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/catalog/school-life")
public class SchoolLifeCatalogController {
    private final SchoolLifeContent content;
    public SchoolLifeCatalogController(SchoolLifeContent content) { this.content = content; }
    @GetMapping
    public ApiResponse<Collection<Map<String, Object>>> sources() { return ApiResponse.ok(content.sources()); }
}
