package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlayerRequest(
        @NotBlank @Size(min = 1, max = 24) String name,
        String applicationProfile
) {
}
