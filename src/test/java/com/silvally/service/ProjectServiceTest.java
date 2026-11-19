package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.ProjectView;
import com.silvally.entity.PlayerProject;
import com.silvally.entity.ProjectStatus;
import com.silvally.repository.PlayerProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProjectServiceTest {

    private static final String API = "REST_API_SERVER";
    private static final String WEB = "WEB_PORTFOLIO_SITE";
    private static final String GAME = "MINI_GAME";

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRules projectRules;

    @Autowired
    private PlayerProjectRepository projectRepository;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private SchoolFixture fixture;

    @Test
    @DisplayName("작업 스냅샷 2개가 쌓이면 완성도를 계산해 완료 처리한다")
    void work_enoughSnapshots_completesWithQuality() {
        UUID id = fixture.enrolledStudent();
        fixture.addStats(id, Map.of("B", 50, "P", 40));
        start(id, API);

        MutationResult first = work(id, API);
        assertThat(first.resultCode()).isEqualTo("WORKED");
        assertThat(first.spentAp()).isEqualTo(1);
        assertThat(first.details()).containsEntry("workCount", 1);

        fixture.advance(id);
        MutationResult second = work(id, API);

        assertThat(second.resultCode()).isEqualTo("COMPLETE");
        assertThat((int) second.details().get("quality")).isBetween(85, 95);
        PlayerProject project = projectRepository.findByPlayerIdAndProjectCode(id, API).orElseThrow();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETE);
        assertThat(projectRules.snapshots(project)).hasSize(2);
        assertThat(achievementService.getProjects(id)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("projectCode", API).containsEntry("workCount", 2));
    }

    @Test
    @DisplayName("완성도가 기준에 못 미치면 실패 처리하고 포트폴리오에 등록할 수 없다")
    void work_lowQuality_failsAndCannotRegister() {
        UUID id = fixture.enrolledStudent();
        start(id, API);
        work(id, API);
        fixture.advance(id);

        MutationResult second = work(id, API);

        assertThat(second.resultCode()).isEqualTo("FAILED");
        assertThat((int) second.details().get("quality")).isLessThan((int) second.details().get("passQuality"));
        assertError(() -> register(id, API), ErrorCode.LOCKED);
    }

    @Test
    @DisplayName("같은 턴에는 한 번만 작업하고, 진행 중 프로젝트가 있으면 다른 프로젝트를 시작할 수 없다")
    void work_sameTurn_andSecondStart_rejected() {
        UUID id = fixture.enrolledStudent();
        start(id, API);
        work(id, API);

        assertError(() -> work(id, API), ErrorCode.WRONG_STEP);
        assertError(() -> start(id, WEB), ErrorCode.LOCKED);
        assertError(() -> work(id, WEB), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("시작 가능한 학년 전에는 프로젝트를 시작할 수 없다")
    void start_beforeMinYear_locked() {
        UUID id = fixture.enrolledStudent();

        ProjectView android = projectService.getProjects(id).stream()
                .filter(p -> p.code().equals("ANDROID_NOTE_APP")).findFirst().orElseThrow();
        assertThat(android.startable()).isFalse();
        assertThat(android.reasons()).anyMatch(r -> r.contains("2학년부터"));
        assertError(() -> start(id, "ANDROID_NOTE_APP"), ErrorCode.LOCKED);
        assertError(() -> start(id, "NO_SUCH_PROJECT"), ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("기간 안에 끝내지 못한 프로젝트는 턴 종료 때 만료되어 더 작업할 수 없다")
    void advanceTurn_afterPeriod_expiresProject() {
        UUID id = fixture.enrolledStudent();
        start(id, API);
        work(id, API);

        MutationResult last = fixture.advanceTimes(id, 10);

        assertThat(last.details()).containsEntry("expiredProjects", java.util.List.of(API));
        assertThat(projectRepository.findByPlayerIdAndProjectCode(id, API).orElseThrow().getStatus())
                .isEqualTo(ProjectStatus.EXPIRED);
        assertError(() -> work(id, API), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("완료 작품은 포트폴리오에 최대 3개까지 등록할 수 있다")
    void registerPortfolio_capacityThree() {
        UUID id = fixture.enrolledStudent();
        completed(id, API, 80);
        completed(id, WEB, 80);
        completed(id, GAME, 80);
        completed(id, "CHATBOT", 80);

        assertThat(register(id, API).resultCode()).isEqualTo("REGISTERED");
        register(id, WEB);
        MutationResult third = register(id, GAME);
        assertThat(third.details()).containsEntry("portfolioCount", ProjectRules.PORTFOLIO_SLOTS);

        assertError(() -> register(id, "CHATBOT"), ErrorCode.LOCKED);
        assertError(() -> register(id, API), ErrorCode.WRONG_STEP);
    }

    @Test
    @DisplayName("포트폴리오 점수는 직무가 같은 작품에 가중치를 주고 최대값을 넘지 않는다")
    void portfolioScore_roleWeightAndCap() {
        UUID id = fixture.enrolledStudent();
        registered(id, API, 80);
        registered(id, GAME, 60);

        // BACKEND 기준: 80 × 1.0 + 60 × 0.5 = 110, 환산 ÷ 2 = 55
        assertThat(projectRules.portfolioScore(fixture.load(id), "BACKEND")).isEqualTo(55);
        // GAME 기준: 80 × 0.5 + 60 × 1.0 = 100, 환산 ÷ 2 = 50
        assertThat(projectRules.portfolioScore(fixture.load(id), "GAME")).isEqualTo(50);

        UUID perfect = fixture.enrolledStudent();
        registered(perfect, API, 100);
        registered(perfect, WEB, 100);
        registered(perfect, GAME, 100);
        assertThat(projectRules.portfolioScore(fixture.load(perfect), "BACKEND")).isEqualTo(ProjectRules.PORTFOLIO_MAX);
        assertThat(projectRules.portfolioScore(fixture.load(fixture.enrolledStudent()), "BACKEND")).isZero();
    }

    private MutationResult start(UUID id, String code) {
        return projectService.start(id, code, fixture.version(id), UUID.randomUUID().toString());
    }

    private MutationResult work(UUID id, String code) {
        return projectService.work(id, code, fixture.version(id), UUID.randomUUID().toString());
    }

    private MutationResult register(UUID id, String code) {
        return projectService.registerPortfolio(id, code, fixture.version(id), UUID.randomUUID().toString());
    }

    private void completed(UUID id, String code, int quality) {
        PlayerProject project = PlayerProject.start(id, code, 1, 1, 4);
        project.finish(quality, true);
        projectRepository.save(project);
    }

    private void registered(UUID id, String code, int quality) {
        PlayerProject project = PlayerProject.start(id, code, 1, 1, 4);
        project.finish(quality, true);
        project.registerPortfolio();
        projectRepository.save(project);
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
