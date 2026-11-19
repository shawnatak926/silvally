package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.response.InterviewView;
import com.silvally.entity.*;
import com.silvally.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InterviewServiceImpl implements InterviewService {
    private final ActivityRecordRepository activities;
    private final InterviewQuestionGenerator generator;
    private final java.util.concurrent.ExecutorService executor;
    private final long timeoutMs;
    private final tools.jackson.databind.ObjectMapper json;
    private final PlayerRepository players;
    private final JobApplicationRepository applications;
    private final InterviewQuestionRepository questions;
    public InterviewServiceImpl(PlayerRepository players, JobApplicationRepository applications,
                                InterviewQuestionRepository questions, ActivityRecordRepository activities,
                                InterviewQuestionGenerator generator,
                                @org.springframework.beans.factory.annotation.Qualifier("interviewQuestionExecutor") java.util.concurrent.ExecutorService executor,
                                @org.springframework.beans.factory.annotation.Value("${app.interview.timeout-ms:7000}") long timeoutMs,
                                tools.jackson.databind.ObjectMapper json) {
        this.activities = activities; this.generator = generator; this.executor = executor;
        this.timeoutMs = Math.min(7000, Math.max(1, timeoutMs)); this.json = json;
        this.players = players; this.applications = applications; this.questions = questions;
    }
    @Override @Transactional
    public InterviewView getSchoolQuestion(UUID playerId) {
        return question(playerId, InterviewKind.SCHOOL, "AD05");
    }
    @Override @Transactional
    public InterviewView getCompanyQuestion(UUID playerId, UUID applicationId) {
        if (applicationId == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        return question(playerId, InterviewKind.COMPANY, applicationId.toString());
    }
    private InterviewView question(UUID playerId, InterviewKind kind, String subject) {
        // 같은 회차의 동시 질문 발급을 직렬화하되 게임 버전은 바꾸지 않습니다.
        Player player = players.findByIdForUpdate(playerId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireContext(player, kind, subject);
        InterviewQuestion q = questions.findByPlayerIdAndKindAndSubjectCode(playerId, kind, subject)
                .orElseGet(() -> generate(playerId, kind, subject));
        if (q.isCompleted()) throw new BusinessException(ErrorCode.WRONG_STEP, "이미 끝난 면접입니다.");
        return new InterviewView(q.getId(), q.getQuestion(), q.getSource(), q.getKind().name());
    }
    private InterviewQuestion generate(UUID playerId, InterviewKind kind, String subject) {
        String base = kind == InterviewKind.SCHOOL
                ? "지원 동기, 직접 수행한 활동 경험과 문제 해결 과정, 입학 후 학습 계획을 구체적으로 설명해 주세요."
                : "지원 직무에 필요한 기술 이해, 프로젝트 경험과 협업 과정, 직무 적합성을 구체적으로 설명해 주세요.";
        var saved = activities.findByPlayerIdOrderByIdAsc(playerId);
        var recent = saved.stream().skip(Math.max(0, saved.size() - 3)).map(activity -> {
            String scene = activity.getEvidenceJson() == null ? "" : json.readTree(activity.getEvidenceJson()).path("scene").asString("");
            String text = activity.getEventCode() + ": " + (scene.isBlank() ? activity.getChoiceCode() : scene);
            return text.substring(0, Math.min(text.length(), 120));
        }).toList();
        var context = new InterviewQuestionGenerator.Context(kind.name(), subject, recent, base);
        var task = executor.submit(() -> generator.generate(context));
        InterviewQuestionGenerator.Generated generated;
        try {
            generated = task.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (generated == null || generated.question() == null || generated.question().isBlank()
                    || generated.question().length() > 1200 || !java.util.Set.of("AI", "MOCK", "FALLBACK").contains(generated.source()))
                throw new IllegalArgumentException("Invalid generated question");
        } catch (Exception e) {
            task.cancel(true);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            generated = new InterviewQuestionGenerator.Generated(base, "FALLBACK");
        }
        return questions.save(new InterviewQuestion(playerId, kind, subject, generated.question(), generated.source()));
    }
    @Override @Transactional
    public com.silvally.dto.response.InterviewValidationResult submitAnswer(Player player, InterviewKind kind,
            String subject, UUID questionId, String answer) {
        // 호출 서비스의 CommandExecutor가 잡은 최신 회차 잠금·트랜잭션 안에서 사용합니다.
        player = players.findByIdForUpdate(player.getId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String normalized = validateAnswer(player, kind, subject, questionId, answer);
        InterviewQuestion q = questions.findByPlayerIdAndKindAndSubjectCode(player.getId(), kind, subject).orElseThrow();
        q.complete(normalized);
        return new com.silvally.dto.response.InterviewValidationResult(q.getId(), kind.name(), subject,
                q.getQuestion(), normalized, q.getSource());
    }
    @Override
    public String validateAnswer(Player player, InterviewKind kind, String subject, UUID questionId, String answer) {
        requireContext(player, kind, subject);
        if (questionId == null || answer == null || answer.isBlank() || answer.length() > 1200)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발급된 질문 ID와 1~1200자의 답변이 필요합니다.");
        InterviewQuestion current = questions.findByPlayerIdAndKindAndSubjectCode(player.getId(), kind, subject)
                .orElseThrow(() -> new BusinessException(ErrorCode.WRONG_STEP, "먼저 면접 질문을 조회해야 합니다."));
        if (current.isCompleted()) throw new BusinessException(ErrorCode.WRONG_STEP, "이미 끝난 면접입니다.");
        if (!current.getId().equals(questionId))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "해당 회차·면접에 발급된 질문 ID가 아닙니다.");
        return answer.strip();
    }
    private void requireContext(Player p, InterviewKind kind, String subject) {
        if (kind == InterviewKind.SCHOOL) {
            if (!"AD05".equals(subject) || p.getPhase() != Phase.ADMISSION_2 || !"AD05".equals(p.getAdmissionEvent()))
                throw new BusinessException(ErrorCode.WRONG_STEP, "학교 면접 단계가 아닙니다.");
            return;
        }
        if (kind != InterviewKind.COMPANY) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        UUID applicationId;
        try { applicationId = UUID.fromString(subject); }
        catch (IllegalArgumentException | NullPointerException e) { throw new BusinessException(ErrorCode.VALIDATION_ERROR); }
        JobApplication application = applications.findById(applicationId)
                .filter(a -> a.getPlayerId().equals(p.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (p.getPhase() != Phase.SCHOOL || p.getYear() != 3 || p.getTurn() != 5
                || application.getStatus() != ApplicationStatus.CODE_PASSED)
            throw new BusinessException(ErrorCode.WRONG_STEP, "코딩 전형을 통과한 3학년 5턴 회사 면접만 가능합니다.");
    }
}
