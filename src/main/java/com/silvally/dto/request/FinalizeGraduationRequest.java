package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record FinalizeGraduationRequest(
        @NotBlank String path,
        UUID applicationId
) {
}
