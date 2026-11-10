package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ApplyJobRequest(
        @NotBlank String companyCode
) {
}
