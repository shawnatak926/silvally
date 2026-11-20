package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.NarshaActionRequest;
import com.silvally.dto.request.NarshaInput;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.NarshaView;
import com.silvally.entity.NarshaProject;
import com.silvally.entity.NarshaStatus;
import com.silvally.entity.Player;
import com.silvally.repository.NarshaProjectRepository;
import com.silvally.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 학년별 나르샤 기록 서비스.
 *
 * <p>{@code Player.narshaStage}·{@code narshaRank}는 가장 최근 프로젝트 상태를 따라가는 요약 값으로 남겨 두어
 * 기존 PlayerView와 일정·스트레스 계산이 그대로 동작한다. 프로젝트 기록의 원본은 {@code narsha_project}다.</p>
 *
 * <p>기존 저장 상태 변환: 이 기능 이전에는 단계 문자열만 있고 프로젝트 행이 없다. 그런 회차가 마무리·순위·전시·작업을
 * 처음 요청하면 현재 단계에 맞는 프로젝트 행을 {@code LEGACY} 주제로 만들어 이어 간다. 2학년 단계였다면 1학년 행도
 * 마무리된 {@code LEGACY} 기록으로 만든다. 조회만으로는 저장 상태를 바꾸지 않는다.</p>
 */
@Service
@Transactional(readOnly = true)
public class NarshaServiceImpl implements NarshaService {
    static final String LEGACY_TOPIC = "기존 기록(주제 정보 없음)";
    private static final Set<String> WORK_CHOICES = Set.of("NARSHA_PAIR", "NARSHA_SCOPE", "NARSHA_EXPAND");
    private static final String MENTOR_CHOICE = "STRESS_MENTOR";
    private static final String KIND_WORK = "WORK";
    private static final String KIND_MENTORING = "MENTORING";
    static final int ACTION_AP = 1;

    private final PlayerRepository players;
    private final NarshaProjectRepository projects;
    private final CommandExecutor commandExecutor;
    private final SchoolRules schoolRules;
    private final NarshaRules rules;

    public NarshaServiceImpl(PlayerRepository players, NarshaProjectRepository projects,
                             CommandExecutor commandExecutor, SchoolRules schoolRules, NarshaRules rules) {
        this.players = players;
        this.projects = projects;
        this.commandExecutor = commandExecutor;
        this.schoolRules = schoolRules;
        this.rules = rules;
    }

    @Override
    public NarshaView getNarsha(UUID playerId) {
        Player player = players.findById(playerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "회차를 찾을 수 없습니다."));
        List<Map<String, Object>> rows = projects(playerId);
        Map<String, Object> current = rows.stream().filter(r -> r.get("year").equals(player.getYear())).findFirst().orElse(null);
        return new NarshaView(rules.topics(), rules.roles(), rules.customTopicRule(), rows, current,
                rules.sources(), NarshaRules.NOTICE);
    }

    @Override
    public List<Map<String, Object>> projects(UUID playerId) {
        return projects.findByPlayerIdOrderByYearAsc(playerId).stream().map(NarshaRules::view).toList();
    }

    @Override
    @Transactional
    public MutationResult act(UUID playerId, NarshaActionRequest request, long expectedVersion, String idempotencyKey) {
        if (request == null || request.kind() == null
                || !Set.of(KIND_WORK, KIND_MENTORING).contains(request.kind())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "kind는 WORK 또는 MENTORING이어야 합니다.");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        String[] result = new String[1];
        return commandExecutor.execute(playerId, expectedVersion, idempotencyKey,
                new CommandSpec<>("NARSHA_ACTION", request.kind(), request, MutationResult.class),
                player -> {
                    schoolRules.requireSchool(player);
                    schoolRules.requireNoPendingChoice(player);
                    if (player.isVacationPending() || player.getPracticeRemaining() > 0) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "방학 중에는 나르샤 작업을 할 수 없습니다.");
                    }
                    NarshaProject project = current(player);
                    if (project == null || project.getStatus() != NarshaStatus.IN_PROGRESS) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "진행 중인 나르샤 프로젝트가 없습니다.");
                    }
                    if (project.getLastActionTurn() != null && project.getLastActionTurn() == player.getTurn()) {
                        throw new BusinessException(ErrorCode.WRONG_STEP, "같은 턴에는 한 번만 작업하거나 멘토링을 받을 수 있습니다.");
                    }
                    if (player.getAp() < ACTION_AP) {
                        throw new BusinessException(ErrorCode.INSUFFICIENT_AP,
                                Map.of("required", ACTION_AP, "remaining", player.getAp()));
                    }
                    player.spendAp(ACTION_AP);
                    if (request.kind().equals(KIND_WORK)) {
                        project.addWork(player.getTurn());
                        result[0] = "WORKED";
                    } else {
                        project.addMentoring(player.getTurn());
                        result[0] = "MENTORED";
                    }
                    details.put("narshaProject", NarshaRules.view(project));
                    details.put("projectedQuality", rules.quality(rules.teamStat(player),
                            project.getWorkCount(), project.getMentoringCount()));
                },
                player -> new MutationResult(player.view(), "NARSHA_ACTION", ACTION_AP, Map.of(), result[0], details));
    }

    @Override
    @Transactional
    public void start(Player player, NarshaInput input, Map<String, Object> details) {
        NarshaRules.Resolved resolved = rules.resolve(input);
        if (projects.findByPlayerIdAndYear(player.getId(), player.getYear()).isPresent()) {
            throw new BusinessException(ErrorCode.WRONG_STEP, "이 학년의 나르샤 프로젝트는 이미 시작했습니다.");
        }
        NarshaProject project = projects.save(NarshaProject.begin(player.getId(), player.getYear(), player.getTurn(),
                resolved.source(), resolved.code(), resolved.topic(), resolved.role()));
        player.setNarshaStage(player.getYear() == 1 ? "STARTED" : "RESUMED");
        details.put("narshaProject", NarshaRules.view(project));
    }

    @Override
    @Transactional
    public void wrap(Player player, Map<String, Object> details) {
        NarshaProject project = requireInProgress(player);
        int quality = rules.quality(rules.teamStat(player), project.getWorkCount(), project.getMentoringCount());
        project.wrap(quality, player.getTurn());
        player.setNarshaStage("WRAPPED");
        details.put("quality", quality);
        details.put("narshaProject", NarshaRules.view(project));
    }

    @Override
    @Transactional
    public void rank(Player player, Map<String, Object> details) {
        NarshaProject project = requireInProgress(player);
        int quality = rules.quality(rules.teamStat(player), project.getWorkCount(), project.getMentoringCount());
        int rank = rules.rank(quality);
        project.rank(quality, rank, NarshaRules.RANK_SOURCE, player.getTurn());
        player.rankNarsha(rank);
        details.put("quality", quality);
        details.put("rank", rank);
        details.put("rankSource", NarshaRules.RANK_SOURCE);
        details.put("canExhibit", rules.canExhibit(rank));
        details.put("narshaProject", NarshaRules.view(project));
    }

    @Override
    @Transactional
    public void exhibit(Player player, Map<String, Object> details) {
        NarshaProject project = current(player);
        if (project == null || project.getStatus() != NarshaStatus.RANKED || !rules.canExhibit(project.getRank())) {
            throw new BusinessException(ErrorCode.LOCKED, "순위 발표에서 상위 " + NarshaRules.EXHIBIT_RANK_LIMIT
                    + "위 안에 든 프로젝트만 소프트웨이브에 참가할 수 있습니다.");
        }
        if (project.isExhibited()) {
            throw new BusinessException(ErrorCode.WRONG_STEP, "이미 소프트웨이브에 참가했습니다.");
        }
        project.exhibit();
        player.attendSoftwarewave();
        details.put("narshaProject", NarshaRules.view(project));
    }

    @Override
    @Transactional
    public void recordSchoolChoice(Player player, String choiceCode) {
        boolean work = WORK_CHOICES.contains(choiceCode);
        if ((!work && !MENTOR_CHOICE.equals(choiceCode)) || !NarshaRules.developing(player)) {
            return;
        }
        projects.findByPlayerIdAndYear(player.getId(), player.getYear())
                .filter(project -> project.getStatus() == NarshaStatus.IN_PROGRESS)
                .ifPresent(project -> {
                    if (work) {
                        project.countWorkFromEvent();
                    } else {
                        project.countMentoringFromEvent();
                    }
                });
    }

    private NarshaProject requireInProgress(Player player) {
        NarshaProject project = current(player);
        if (project == null || project.getStatus() != NarshaStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.WRONG_STEP, "진행 중인 나르샤 프로젝트가 없습니다.");
        }
        return project;
    }

    /** 현재 학년 프로젝트. 행이 없고 기존 단계만 남은 회차는 {@code LEGACY} 행으로 변환한다. */
    private NarshaProject current(Player player) {
        return projects.findByPlayerIdAndYear(player.getId(), player.getYear())
                .orElseGet(() -> convertLegacy(player));
    }

    private NarshaProject convertLegacy(Player player) {
        String stage = player.getNarshaStage();
        int year = player.getYear();
        NarshaStatus status = switch (year) {
            case 1 -> "STARTED".equals(stage) ? NarshaStatus.IN_PROGRESS : "WRAPPED".equals(stage) ? NarshaStatus.WRAPPED : null;
            case 2 -> "RESUMED".equals(stage) ? NarshaStatus.IN_PROGRESS : "RANKED".equals(stage) ? NarshaStatus.RANKED : null;
            default -> null;
        };
        if (status == null) {
            return null;
        }
        if (year == 2 && projects.findByPlayerIdAndYear(player.getId(), 1).isEmpty()) {
            NarshaProject first = legacy(player, 1, 8);
            first.wrap(null, 23);
            projects.save(first);
        }
        NarshaProject project = legacy(player, year, year == 1 ? 8 : 2);
        if (status == NarshaStatus.RANKED) {
            int rank = player.getNarshaRank() == null ? 0 : player.getNarshaRank();
            project.rank(null, rank, NarshaRules.RANK_SOURCE, player.getTurn());
            if (player.isSoftwarewaveAttended()) {
                project.exhibit();
            }
        } else if (status == NarshaStatus.WRAPPED) {
            project.wrap(null, player.getTurn());
        }
        return projects.save(project);
    }

    private static NarshaProject legacy(Player player, int year, int startTurn) {
        return NarshaProject.begin(player.getId(), year, startTurn, NarshaProject.SOURCE_LEGACY, null,
                LEGACY_TOPIC, NarshaRules.DEFAULT_ROLE);
    }
}
