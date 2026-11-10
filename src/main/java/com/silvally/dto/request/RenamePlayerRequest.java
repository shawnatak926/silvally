package com.silvally.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenamePlayerRequest(
        @NotBlank @Size(min = 1, max = 24) String name
) {
}
