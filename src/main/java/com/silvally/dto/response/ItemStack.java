package com.silvally.dto.response;

public record ItemStack(
        String itemCode,
        String name,
        int quantity,
        boolean enabled,
        String reason
) {
}
