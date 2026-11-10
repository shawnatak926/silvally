package com.silvally.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplicationView(
        UUID id,
        String companyCode,
        String role,
        String status,
        BigDecimal d,
        BigDecimal k,
        BigDecimal j,
        BigDecimal p,
        String failureCode
) {
}
