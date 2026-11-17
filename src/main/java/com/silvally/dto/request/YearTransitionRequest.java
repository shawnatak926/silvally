package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

public record YearTransitionRequest(
        @NotBlank String choiceCode
) {
}
