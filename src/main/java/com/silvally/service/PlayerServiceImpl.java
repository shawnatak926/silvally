package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.RenamePlayerRequest;
import com.silvally.dto.response.DeleteResult;
import com.silvally.dto.response.HistoryPage;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.PlayerView;
import com.silvally.dto.response.ProgressView;
import com.silvally.dto.response.TurnReadinessView;
import com.silvally.entity.Player;
import com.silvally.repository.ActivityRecordRepository;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.EventCommandRepository;
import com.silvally.repository.JobApplicationRepository;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerProjectRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PlayerServiceImpl implements PlayerService {

    static final String DEFAULT_PROFILE = "OPEN";
    static final Set<String> PROFILES = Set.of("OPEN", "MAKER", "REGION", "ACCESS");

    private final com.silvally.repository.FieldLearningRepository fieldLearning;
    private final com.silvally.repository.ExamResultRepository examResults;
    private final com.silvally.repository.InterviewQuestionRepository interviewQuestions;
    private final PlayerRepository playerRepository;
    private final CommandLogRepository commandLogRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final PlayerProjectRepository playerProjectRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final EventCommandRepository eventCommandRepository;
    private final LifeNarrativeRepository lifeNarrativeRepository;
    private final CommandExecutor commandExecutor;
    private final SchoolTurnService schoolTurnService;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ProgressAssembler progressAssembler;

    public PlayerServiceImpl(PlayerRepository playerRepository,
                             CommandLogRepository commandLogRepository,
                             CourseEnrollmentRepository courseEnrollmentRepository,
                             JobApplicationRepository jobApplicationRepository,
                             PlayerProjectRepository playerProjectRepository,
                             ActivityRecordRepository activityRecordRepository,
                             EventCommandRepository eventCommandRepository,
                             LifeNarrativeRepository lifeNarrativeRepository,
                             CommandExecutor commandExecutor,
                             SchoolTurnService schoolTurnService,
                             HistoryService historyService,
                             ObjectMapper objectMapper,
                             PlatformTransactionManager transactionManager,
                             com.silvally.repository.InterviewQuestionRepository interviewQuestions,
                             com.silvally.repository.FieldLearningRepository fieldLearning,
                             com.silvally.repository.ExamResultRepository examResults,
                             ProgressAssembler progressAssembler) {
        this.progressAssembler = progressAssembler;
        this.examResults = examResults;
        this.fieldLearning = fieldLearning;
        this.interviewQuestions = interviewQuestions;
        this.playerRepository = playerRepository;
        this.commandLogRepository = commandLogRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.playerProjectRepository = playerProjectRepository;
        this.activityRecordRepository = activityRecordRepository;
        this.eventCommandRepository = eventCommandRepository;
        this.lifeNarrativeRepository = lifeNarrativeRepository;
        this.commandExecutor = commandExecutor;
        this.schoolTurnService = schoolTurnService;
        this.historyService = historyService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 회차 생성. 같은 Idempotency-Key 재요청은 처음 만든 회차 응답을 그대로 돌려준다.
     * 동시에 같은 키로 들어와 유니크 제약에 걸리면 먼저 저장된 회차 응답으로 복구한다.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlayerView create(String idempotencyKey, CreatePlayerRequest request) {
        String createKey = CommandExecutor.parseIdempotencyKey(idempotencyKey).toString();
        String profile = normalizeProfile(request.applicationProfile());
        String requestHash = commandExecutor.hash(new CommandSpec<>("CREATE_PLAYER", "PLAYER",
                new CreatePlayerRequest(request.name(), profile), PlayerView.class));

        Optional<PlayerView> replay = transactionTemplate.execute(status -> replayCreate(createKey, requestHash));
        if (replay.isPresent()) {
            return replay.get();
        }
        try {
            return transactionTemplate.execute(status -> insert(request.name(), profile, createKey, requestHash));
        } catch (DataIntegrityViolationException e) {
            return transactionTemplate.execute(status -> replayCreate(createKey, requestHash))
                    .orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT));
        }
    }

    @Override
    public PlayerView get(UUID playerId) {
        return findPlayer(playerId).view();
    }

    @Override
    @Transactional
    public PlayerView rename(UUID playerId, long expectedVersion, String idempotencyKey, RenamePlayerRequest request) {
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("RENAME", "PLAYER", request, PlayerView.class),
                player -> player.rename(request.name()),
                Player::view);
    }

    @Override
    @Transactional
    public DeleteResult delete(UUID playerId, long expectedVersion) {
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(PlayerServiceImpl::notFound);
        CommandExecutor.verifyVersion(player, expectedVersion);

        fieldLearning.deleteById(playerId);
        examResults.deleteByPlayerId(playerId);
        interviewQuestions.deleteByPlayerId(playerId);
        lifeNarrativeRepository.deleteByPlayerId(playerId);
        commandLogRepository.deleteByPlayerId(playerId);
        courseEnrollmentRepository.deleteByPlayerId(playerId);
        jobApplicationRepository.deleteByPlayerId(playerId);
        playerProjectRepository.deleteByPlayerId(playerId);
        activityRecordRepository.deleteByPlayerId(playerId);
        eventCommandRepository.deleteByPlayerId(playerId);
        playerRepository.delete(player);
        return new DeleteResult(playerId, true);
    }

    @Override
    public ProgressView getProgress(UUID playerId) {
        return progressAssembler.assemble(findPlayer(playerId));
    }

    @Override
    public HistoryPage getHistory(UUID playerId, long afterId, int limit) {
        return historyService.getHistory(playerId, afterId, limit);
    }

    @Override
    @Transactional
    public MutationResult advanceTurn(UUID playerId, long expectedVersion, String idempotencyKey) {
        return schoolTurnService.advanceTurn(playerId, expectedVersion, idempotencyKey);
    }

    @Override
    public TurnReadinessView getTurnReadiness(UUID playerId) {
        return schoolTurnService.getTurnReadiness(playerId);
    }

    private Optional<PlayerView> replayCreate(String createKey, String requestHash) {
        return playerRepository.findByCreateKey(createKey).map(player -> {
            if (!requestHash.equals(player.getCreateRequestHash())) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            return objectMapper.readValue(player.getCreateResponseJson(), PlayerView.class);
        });
    }

    private PlayerView insert(String name, String profile, String createKey, String requestHash) {
        Player player = playerRepository.saveAndFlush(
                Player.create(name, profile, createKey, requestHash));
        PlayerView view = player.view();
        playerRepository.saveCreateResponse(player.getId(), objectMapper.writeValueAsString(view));
        return view;
    }

    private static String normalizeProfile(String applicationProfile) {
        if (applicationProfile == null || applicationProfile.isBlank()) {
            return DEFAULT_PROFILE;
        }
        if (!PROFILES.contains(applicationProfile)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "applicationProfile은 OPEN, MAKER, REGION, ACCESS 중 하나여야 합니다.");
        }
        return applicationProfile;
    }

    private Player findPlayer(UUID playerId) {
        return playerRepository.findById(playerId).orElseThrow(PlayerServiceImpl::notFound);
    }

    private static BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다.");
    }
}
