package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.entity.CommandLog;
import com.silvally.entity.Player;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 회차 상태를 바꾸는 모든 커맨드의 공통 실행기.
 *
 * <ol>
 *     <li>회차 행을 비관적 락으로 잡아 같은 회차의 커맨드를 한 번에 하나씩 처리한다.</li>
 *     <li>같은 Idempotency-Key가 이미 처리됐으면 저장된 응답을 그대로 돌려주고 다시 반영하지 않는다.
 *     요청 내용이 다르면 IDEMPOTENCY_CONFLICT.</li>
 *     <li>X-Expected-Version이 현재 버전과 다르면 VERSION_CONFLICT.</li>
 *     <li>커맨드를 반영하고 버전을 올린 뒤 응답을 command_log에 저장한다.</li>
 * </ol>
 */
@Component
public class CommandExecutor {

    private final PlayerRepository playerRepository;
    private final CommandLogRepository commandLogRepository;
    private final ObjectMapper objectMapper;

    public CommandExecutor(PlayerRepository playerRepository,
                           CommandLogRepository commandLogRepository,
                           ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.commandLogRepository = commandLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> T execute(UUID playerId, long expectedVersion, String idempotencyKey, CommandSpec<T> spec,
                         Consumer<Player> mutation, Function<Player, T> responder) {
        UUID commandId = parseIdempotencyKey(idempotencyKey);
        String requestHash = hash(spec);

        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));

        Optional<T> replay = replayIfProcessed(playerId, commandId, requestHash, spec.responseType());
        if (replay.isPresent()) {
            return replay.get();
        }

        verifyVersion(player, expectedVersion);

        mutation.accept(player);
        // 커맨드마다 lastCommandId가 달라 엔티티가 항상 변경되므로 @Version이 정확히 1 증가한다.
        player.markCommand(commandId);
        playerRepository.flush();

        T response = responder.apply(player);
        commandLogRepository.save(CommandLog.of(player, commandId, spec.kind(), spec.subjectCode(),
                requestHash, 200, toJson(response), toJson(player.getStats().toMap())));
        return response;
    }

    public static void verifyVersion(Player player, long expectedVersion) {
        if (player.getVersion() != expectedVersion) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, Map.of(
                    "expectedVersion", expectedVersion,
                    "currentVersion", player.getVersion()));
        }
    }

    public static UUID parseIdempotencyKey(String idempotencyKey) {
        try {
            return UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key는 UUID 형식이어야 합니다.");
        }
    }

    public String hash(CommandSpec<?> spec) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("kind", spec.kind());
        source.put("subjectCode", spec.subjectCode());
        source.put("payload", spec.payload());
        return sha256(toJson(source));
    }

    private <T> Optional<T> replayIfProcessed(UUID playerId, UUID commandId, String requestHash, Class<T> type) {
        return commandLogRepository.findByPlayerIdAndCommandId(playerId, commandId)
                .map(log -> {
                    if (!log.getRequestHash().equals(requestHash)) {
                        throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
                    }
                    return objectMapper.readValue(log.getResponseJson(), type);
                });
    }

    private String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
