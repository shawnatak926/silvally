package com.silvally.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class CommandExecutor {
    @Transactional
    public <T> T execute(UUID playerId, long expectedVersion, String idempotencyKey, Supplier<T> command) {
        throw new UnsupportedOperationException("TODO: 공통 커맨드 실행");
    }
}
