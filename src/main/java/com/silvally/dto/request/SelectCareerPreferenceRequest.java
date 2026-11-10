package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SelectCareerPreferenceRequest(
        @NotBlank String roleCode
) {
}
