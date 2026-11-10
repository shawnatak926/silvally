package com.silvally.dto.response;

import java.util.UUID;

public record DeleteResult(UUID id, boolean deleted) {
}
