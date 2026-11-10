package com.silvally.dto.response;

public record CareerPreferenceMutation(
        PlayerView player,
        CareerPreferenceView preference
) {
}
