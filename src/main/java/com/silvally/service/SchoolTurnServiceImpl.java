package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SchoolTurnServiceImpl implements SchoolTurnService {

    private final CommandExecutor commandExecutor;
    private final SchoolRules rules;

    public SchoolTurnServiceImpl(CommandExecutor commandExecutor, SchoolRules rules) {
        this.commandExecutor = commandExecutor;
        this.rules = rules;
    }

    @Override
    @Transactional
    public MutationResult advanceTurn(UUID playerId, long expectedVersion, String idempotencyKey) {
        TurnOutcome outcome = new TurnOutcome();
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("TURN", "TURN_END", Map.of(), MutationResult.class),
                player -> advance(player, outcome),
                player -> new MutationResult(player.view(), "TURN_END", 0,
                        outcome.statDelta, outcome.resultCode, outcome.details(player)));
    }

    private void advance(Player player, TurnOutcome outcome) {
        if (player.getPhase() != Phase.SCHOOL) {
            throw new BusinessException(ErrorCode.WRONG_PHASE, "재학 중에만 턴을 진행할 수 있습니다.");
        }
        outcome.statDelta = rules.turnGrowth(player);
        player.getStats().apply(outcome.statDelta);

        if (!rules.isLastTurnOfYear(player)) {
            player.nextTurn(SchoolRules.AP_PER_TURN);
            outcome.resultCode = "TURN_ADVANCED";
        } else if (player.getYear() >= SchoolRules.LAST_YEAR) {
            player.readyToGraduate();
            outcome.resultCode = "GRADUATION_READY";
        } else {
            player.nextYear(SchoolRules.AP_PER_TURN);
            outcome.resultCode = "YEAR_ADVANCED";
        }
    }

    private static final class TurnOutcome {
        private Map<String, Integer> statDelta = Map.of();
        private String resultCode;

        private Map<String, Object> details(Player player) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("year", player.getYear());
            details.put("turn", player.getTurn());
            return details;
        }
    }
}
