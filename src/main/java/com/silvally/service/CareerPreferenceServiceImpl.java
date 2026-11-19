package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.SelectCareerPreferenceRequest;
import com.silvally.dto.response.CareerPreferenceMutation;
import com.silvally.dto.response.CareerPreferenceView;
import com.silvally.entity.ActivityRecord;
import com.silvally.entity.EventChoice;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import com.silvally.repository.ActivityRecordRepository;
import com.silvally.repository.EventChoiceRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareerPreferenceServiceImpl implements CareerPreferenceService {

    private static final String BEFORE_SCHOOL_EVENT_PREFIX = "BS";

    private final PlayerRepository playerRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final EventChoiceRepository eventChoiceRepository;
    private final CommandExecutor commandExecutor;
    private final CareerRoleCatalog roleCatalog;
    private final CareerRecommender recommender;
    private final ObjectMapper objectMapper;

    public CareerPreferenceServiceImpl(PlayerRepository playerRepository,
                                       ActivityRecordRepository activityRecordRepository,
                                       EventChoiceRepository eventChoiceRepository,
                                       CommandExecutor commandExecutor,
                                       CareerRoleCatalog roleCatalog,
                                       CareerRecommender recommender,
                                       ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.activityRecordRepository = activityRecordRepository;
        this.eventChoiceRepository = eventChoiceRepository;
        this.commandExecutor = commandExecutor;
        this.roleCatalog = roleCatalog;
        this.recommender = recommender;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CareerPreferenceView getPreference(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));
        requirePreferencePhase(player);
        return view(player, loadRecord(player, true));
    }

    @Override
    @Transactional
    public CareerPreferenceMutation select(UUID playerId, long expectedVersion, String idempotencyKey,
                                           SelectCareerPreferenceRequest request) {
        throw new UnsupportedOperationException("TODO: 희망 직무 확정·변경");
    }

    private static void requirePreferencePhase(Player player) {
        if (player.getPhase() != Phase.CAREER_PICK && player.getPhase() != Phase.SCHOOL
                && player.getPhase() != Phase.GRAD_READY) {
            throw new BusinessException(ErrorCode.WRONG_PHASE, "비포스쿨을 마친 뒤에 확인할 수 있습니다.");
        }
    }

    /**
     * 저장된 기록을 읽고, 없으면 현재 활동·능력치로 최초 추천을 계산해 저장한다.
     * 저장한 뒤에는 능력치가 자라도 최초 추천은 바뀌지 않는다.
     */
    private CareerPreferenceRecord loadRecord(Player player, boolean persistWithoutVersionBump) {
        if (player.getRecommendationJson() != null) {
            return objectMapper.readValue(player.getRecommendationJson(), CareerPreferenceRecord.class);
        }
        CareerPreferenceRecord record = new CareerPreferenceRecord(player.getStats().toMap(),
                recommender.recommend(roleCatalog.all(player), player.getStats().toMap(), activities(player)),
                new ArrayList<>());
        if (persistWithoutVersionBump) {
            playerRepository.saveRecommendationIfAbsent(player.getId(), objectMapper.writeValueAsString(record));
        } else {
            player.storeRecommendation(objectMapper.writeValueAsString(record));
        }
        return record;
    }

    private CareerPreferenceView view(Player player, CareerPreferenceRecord record) {
        return new CareerPreferenceView(player.getInitialCareerCode(), player.getCurrentCareerCode(),
                record.recommendations(), false, List.of());
    }

    private List<CareerRecommender.Activity> activities(Player player) {
        List<CareerRecommender.Activity> result = new ArrayList<>();
        for (ActivityRecord record : activityRecordRepository.findByPlayerIdOrderByIdAsc(player.getId())) {
            if (!record.getEventCode().startsWith(BEFORE_SCHOOL_EVENT_PREFIX)) {
                continue;
            }
            Set<String> tags = new LinkedHashSet<>();
            if (record.getEvidenceJson() != null) {
                JsonNode evidence = objectMapper.readTree(record.getEvidenceJson());
                evidence.path("tags").forEach(node -> tags.add(node.asString()));
            }
            String label = eventChoiceRepository.findById(new EventChoice.Id(player.getRulesVersion(),
                            record.getEventCode(), record.getChoiceCode()))
                    .map(EventChoice::getLabel).orElse(record.getChoiceCode());
            result.add(new CareerRecommender.Activity(record.getBeforeSchoolDay() == null ? 0
                    : record.getBeforeSchoolDay(), record.getEventCode(), record.getChoiceCode(), label, tags));
        }
        return result;
    }
}
