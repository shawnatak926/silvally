package com.silvally.dto.response;

public record ApplicationMutation(
        ApplicationView application,
        MutationResult change
) {
}
