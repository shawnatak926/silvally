package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChooseEventRequest(
        @NotBlank String choiceCode
) {
}
