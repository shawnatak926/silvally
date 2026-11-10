package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PerformActionRequest(
        @NotBlank String actionCode,
        String target
) {
}
