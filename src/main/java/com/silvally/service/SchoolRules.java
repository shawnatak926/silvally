package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.YearTransitionView;
import com.silvally.entity.CatalogDef;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import com.silvally.repository.CatalogDefRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SchoolRules {
    public static final int TURNS_PER_YEAR = 10;
    public static final int AP_PER_TURN = 3;
    public static final int LAST_YEAR = 3;
    static final List<String> BASE_FOCUS = List.of("P", "A");
    static final int REST_H_PER_AP = 2;
    static final int FATIGUE_H = 2;

    private final CatalogDefRepository catalog;
    private final ObjectMapper objectMapper;

    public SchoolRules(CatalogDefRepository catalog, ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    public List<String> focusStats(Player player) {
        List<String> focus = new ArrayList<>();
        if (player.getMajor() == null) {
            focus.addAll(BASE_FOCUS);
        } else {
            focus.addAll(focusOf(player, CatalogKind.MAJOR, player.getMajor()));
        }
        if (player.getTrack() != null) {
            focus.addAll(focusOf(player, CatalogKind.TRACK, player.getTrack()));
        }
        return focus;
    }

    public Map<String, Integer> turnGrowth(Player player) {
        Map<String, Integer> delta = new LinkedHashMap<>();
        focusStats(player).forEach(stat -> delta.merge(stat, 1, Integer::sum));
        int restH = player.getAp() > 0 ? player.getAp() * REST_H_PER_AP : -FATIGUE_H;
        delta.merge("H", restH, Integer::sum);
        return delta;
    }

    /** 입학 후 누적 턴(1학년 1턴 = 1). 프로젝트 기간·채용 마감처럼 학년을 넘는 기간 계산에 쓴다. */
    public int absoluteTurn(Player player) {
        return (player.getYear() - 1) * TURNS_PER_YEAR + player.getTurn();
    }

    public boolean isLastTurnOfYear(Player player) {
        return player.getTurn() >= TURNS_PER_YEAR;
    }

    public Optional<CatalogKind> choiceRequiredFor(int newYear) {
        return switch (newYear) {
            case 2 -> Optional.of(CatalogKind.MAJOR);
            case 3 -> Optional.of(CatalogKind.TRACK);
            default -> Optional.empty();
        };
    }

    public void requireSchool(Player player) {
        if (player.getPhase() != Phase.SCHOOL) {
            throw new BusinessException(ErrorCode.WRONG_PHASE, "재학 중에만 요청할 수 있습니다.");
        }
    }

    public void requireNoPendingChoice(Player player) {
        if (player.getPendingChoice() == null) {
            return;
        }
        YearTransitionView view = transitionView(player);
        throw new BusinessException(ErrorCode.CHOICE_REQUIRED, Map.of(
                "pendingChoice", view.pendingChoice(),
                "options", view.options().stream().map(YearTransitionView.Option::code).toList()));
    }

    public YearTransitionView transitionView(Player player) {
        String pending = player.getPendingChoice();
        List<YearTransitionView.Option> options = pending == null ? List.of()
                : catalog.findByKind(player.getRulesVersion(), pending).stream()
                .map(def -> new YearTransitionView.Option(def.getId().code(), def.getTitle(), focus(def)))
                .toList();
        return new YearTransitionView(player.getYear(), pending, options);
    }

    public CatalogDef findChoice(Player player, CatalogKind kind, String code) {
        return catalog.findById(new CatalogDef.Id(player.getRulesVersion(), kind.name(), code))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "선택할 수 없는 " + kind.getPath() + " 코드입니다: " + code));
    }

    private List<String> focusOf(Player player, CatalogKind kind, String code) {
        return catalog.findById(new CatalogDef.Id(player.getRulesVersion(), kind.name(), code))
                .map(this::focus)
                .orElse(List.of());
    }

    private List<String> focus(CatalogDef def) {
        JsonNode focus = objectMapper.readTree(def.getConfigJson()).path("focus");
        List<String> stats = new ArrayList<>();
        focus.forEach(node -> stats.add(node.asString()));
        return stats;
    }
}
