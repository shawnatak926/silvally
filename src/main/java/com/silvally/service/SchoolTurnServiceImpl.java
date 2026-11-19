package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.YearTransitionRequest;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.YearTransitionView;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.Player;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SchoolTurnServiceImpl implements SchoolTurnService {

    private final PlayerRepository playerRepository;
    private final CommandExecutor commandExecutor;
    private final SchoolRules rules;
    private final CertificateService certificateService;
    private final ProjectService projectService;

    public SchoolTurnServiceImpl(PlayerRepository playerRepository, CommandExecutor commandExecutor,
                                 SchoolRules rules, CertificateService certificateService,
                                 ProjectService projectService) {
        this.playerRepository = playerRepository;
        this.commandExecutor = commandExecutor;
        this.rules = rules;
        this.certificateService = certificateService;
        this.projectService = projectService;
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

    @Override
    public YearTransitionView getYearTransition(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));
        return rules.transitionView(player);
    }

    @Override
    @Transactional
    public MutationResult chooseYearTransition(UUID playerId, long expectedVersion, String idempotencyKey,
                                               YearTransitionRequest request) {
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("YEAR_CHOICE", request.choiceCode(), request, MutationResult.class),
                player -> choose(player, request.choiceCode()),
                player -> new MutationResult(player.view(), "YEAR_CHOICE", 0, Map.of(),
                        "CHOSEN", Map.of("major", nullable(player.getMajor()), "track", nullable(player.getTrack()))));
    }

    private void choose(Player player, String code) {
        rules.requireSchool(player);
        if (player.getPendingChoice() == null) {
            throw new BusinessException(ErrorCode.WRONG_STEP, "지금은 학년 전환 선택이 필요하지 않습니다.");
        }
        CatalogKind kind = CatalogKind.valueOf(player.getPendingChoice());
        rules.findChoice(player, kind, code);
        if (kind == CatalogKind.MAJOR) {
            player.chooseMajor(code);
        } else {
            player.chooseTrack(code);
        }
    }

    private void advance(Player player, TurnOutcome outcome) {
        rules.requireSchool(player);
        rules.requireNoPendingChoice(player);
        outcome.statDelta = rules.turnGrowth(player);
        player.getStats().apply(outcome.statDelta);
        outcome.expiredProjects = projectService.expireOverdue(player, rules.absoluteTurn(player) + 1);

        if (!rules.isLastTurnOfYear(player)) {
            player.nextTurn(SchoolRules.AP_PER_TURN);
            outcome.resultCode = "TURN_ADVANCED";
            return;
        }
        outcome.expiredCourses = certificateService.expireAtYearEnd(player, player.getYear() + 1);
        if (player.getYear() >= SchoolRules.LAST_YEAR) {
            player.readyToGraduate();
            outcome.resultCode = "GRADUATION_READY";
        } else {
            player.nextYear(SchoolRules.AP_PER_TURN);
            rules.choiceRequiredFor(player.getYear()).ifPresent(kind -> player.requireChoice(kind.name()));
            outcome.resultCode = "YEAR_ADVANCED";
        }
    }

    private static String nullable(String value) {
        return value == null ? "" : value;
    }

    private static final class TurnOutcome {
        private Map<String, Integer> statDelta = Map.of();
        private String resultCode;
        private List<String> expiredCourses = List.of();
        private List<String> expiredProjects = List.of();

        private Map<String, Object> details(Player player) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("year", player.getYear());
            details.put("turn", player.getTurn());
            if (!expiredCourses.isEmpty()) {
                details.put("expiredCourses", expiredCourses);
            }
            if (!expiredProjects.isEmpty()) {
                details.put("expiredProjects", expiredProjects);
            }
            if (player.getPendingChoice() != null) {
                details.put("pendingChoice", player.getPendingChoice());
            }
            return details;
        }
    }
}
