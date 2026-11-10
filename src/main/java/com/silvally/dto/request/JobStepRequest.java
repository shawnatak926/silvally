package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JobStepRequest(
        @NotBlank String step
) {
}
