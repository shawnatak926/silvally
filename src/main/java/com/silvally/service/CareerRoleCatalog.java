package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.entity.CatalogDef;
import com.silvally.entity.CatalogKind;
import com.silvally.entity.Player;
import com.silvally.repository.CatalogDefRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** CAREER_ROLE 카탈로그를 직무 규칙으로 읽는다. */
@Component
public class CareerRoleCatalog {

    private final CatalogDefRepository catalog;
    private final ObjectMapper objectMapper;

    public CareerRoleCatalog(CatalogDefRepository catalog, ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    /** 고정 순번 순서의 직무 전체. */
    public List<CareerRole> all(Player player) {
        return catalog.findByKind(player.getRulesVersion(), CatalogKind.CAREER_ROLE.name()).stream()
                .map(this::toRole)
                .sorted(Comparator.comparingInt(CareerRole::order))
                .toList();
    }

    public CareerRole find(Player player, String roleCode) {
        return all(player).stream()
                .filter(role -> role.code().equals(roleCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "알 수 없는 직무 코드입니다: " + roleCode));
    }

    private CareerRole toRole(CatalogDef def) {
        JsonNode config = objectMapper.readTree(def.getConfigJson());
        Map<String, Integer> stats = new LinkedHashMap<>();
        config.path("stats").properties().forEach(e -> stats.put(e.getKey(), e.getValue().asInt()));
        Set<String> tags = new LinkedHashSet<>();
        config.path("tags").forEach(node -> tags.add(node.asString()));
        String summary = config.path("summary").asString(null);
        return new CareerRole(def.getId().code(), def.getTitle(), config.path("order").asInt(), stats, tags, summary);
    }
}
