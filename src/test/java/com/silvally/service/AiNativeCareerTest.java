package com.silvally.service;

import com.silvally.dto.request.ApplyJobRequest;
import com.silvally.dto.request.FinalizeGraduationRequest;
import com.silvally.dto.request.JobStepRequest;
import com.silvally.dto.request.SelectCareerPreferenceRequest;
import com.silvally.dto.response.ApplicationMutation;
import com.silvally.dto.response.CareerPreferenceView;
import com.silvally.dto.response.CareerPreferenceView.Recommendation;
import com.silvally.dto.response.EndingMutation;
import com.silvally.entity.PlayerProject;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import com.silvally.repository.PlayerProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiNativeCareerTest {

    private static final String LONG_ANSWER = "저는 AI 도구와 모델을 활용해 서비스를 직접 설계하고 구현했습니다. ".repeat(10);

    @Autowired
    private CareerRoleCatalog roleCatalog;
    @Autowired
    private CareerRecommender recommender;
    @Autowired
    private CareerPreferenceService preferenceService;
    @Autowired
    private CareerService careerService;
    @Autowired
    private InterviewService interviewService;
    @Autowired
    private PlayerProjectRepository projectRepository;
    @Autowired
    private SchoolFixture fixture;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("직무 카탈로그는 11종이고 코드·순번이 겹치지 않으며 두 AI 직무가 따로 표시된다")
    void catalog_elevenRolesWithDistinctAiRoles() {
        Player player = fixture.load(fixture.enrolledStudent());

        List<CareerRole> roles = roleCatalog.all(player);

        assertThat(roles).hasSize(11);
        assertThat(roles.stream().map(CareerRole::code).collect(java.util.stream.Collectors.toSet())).hasSize(11);
        assertThat(roles).extracting(CareerRole::order).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        CareerRole ai = roleCatalog.find(player, "AI");
        CareerRole aiNative = roleCatalog.find(player, "AI_NATIVE");
        assertThat(ai.label()).isEqualTo("인공지능 개발자");
        assertThat(aiNative.label()).isEqualTo("AI 네이티브 개발자");
        assertThat(ai.stats()).isNotEqualTo(aiNative.stats());
        assertThat(ai.tags()).isNotEqualTo(aiNative.tags());
        assertThat(ai.summary()).contains("모델").contains("데이터");
        assertThat(aiNative.summary()).contains("서비스").contains("구현");
    }

    @Test
    @DisplayName("전체 직무는 정확히 11개이고 같은 입력이면 순서가 바뀌지 않으며 점수가 모두 같으면 고정 순번을 따른다")
    void recommend_allElevenAndStable() {
        Player player = fixture.load(fixture.enrolledStudent());
        List<CareerRole> roles = roleCatalog.all(player);
        Map<String, Integer> stats = Map.of("P", 0, "F", 0, "B", 0, "I", 0, "A", 0, "C", 0);

        List<Recommendation> first = recommender.recommend(roles, stats, List.of());
        List<Recommendation> second = recommender.recommend(new ArrayList<>(roles.reversed()), stats, List.of());

        assertThat(first).hasSize(11).isEqualTo(second);
        assertThat(first).extracting(Recommendation::roleCode).containsExactly("WEB", "BACKEND", "CROSS_PLATFORM", "ANDROID", "IOS", "GAME", "NETWORK", "SECURITY", "AI", "DESIGN", "AI_NATIVE");
        assertThat(recommender.scoreAll(roles, stats, List.of())).hasSize(11)
                .last().satisfies(scored -> assertThat(scored.role().code()).isEqualTo("AI_NATIVE"));
    }

    @Test
    @DisplayName("모델·데이터에 강한 능력치와 AI 활동은 인공지능 개발자를 1순위로 추천하고 이유를 남긴다")
    void recommend_modelAndDataLeaning_aiFirst() {
        Player player = fixture.load(fixture.enrolledStudent());
        Map<String, Integer> stats = Map.of("P", 20, "F", 8, "B", 8, "I", 90, "A", 70, "C", 20);
        List<CareerRecommender.Activity> activities = List.of(
                activity(5, "BS05", "AI", "AI 개발 상담", "ai"),
                activity(1, "BS01", "CLASSROOM", "교실", "study"));

        List<Recommendation> result = recommender.recommend(roleCatalog.all(player), stats, activities);

        assertThat(result).hasSize(11);
        Recommendation top = result.getFirst();
        assertThat(top.roleCode()).isEqualTo("AI");
        assertThat(top.label()).isEqualTo("인공지능 개발자");
        assertThat(top.summary()).isNotBlank();
        assertThat(top.statReasons()).extracting(CareerPreferenceView.StatReason::statCode).containsExactly("I", "A");
        assertThat(top.activityReasons()).extracting(CareerPreferenceView.ActivityReason::choiceCode)
                .containsExactly("AI", "CLASSROOM");
        // AI 네이티브도 점수와 이유를 따로 계산한다. 구현 능력이 약하면 인공지능 개발자보다 뒤에 선다.
        var scored = recommender.scoreAll(roleCatalog.all(player), stats, activities);
        assertThat(rankOf(scored, "AI")).isLessThan(rankOf(scored, "AI_NATIVE"));
    }

    @Test
    @DisplayName("AI 도구로 서비스를 구현하는 능력치와 프로젝트 활동은 AI 네이티브 개발자를 1순위로 추천하고 이유를 남긴다")
    void recommend_serviceBuildingLeaning_aiNativeFirst() {
        Player player = fixture.load(fixture.enrolledStudent());
        Map<String, Integer> stats = Map.of("P", 90, "F", 8, "B", 8, "I", 40, "A", 20, "C", 80);
        List<CareerRecommender.Activity> activities = List.of(
                activity(5, "BS05", "AI", "AI 개발 상담", "ai"),
                activity(4, "BS04", "DEVELOP", "개발", "project", "backend"));

        List<Recommendation> result = recommender.recommend(roleCatalog.all(player), stats, activities);

        assertThat(result).hasSize(11);
        Recommendation top = result.getFirst();
        assertThat(top.roleCode()).isEqualTo("AI_NATIVE");
        assertThat(top.label()).isEqualTo("AI 네이티브 개발자");
        assertThat(top.statReasons()).extracting(CareerPreferenceView.StatReason::statCode).containsExactly("P", "I", "C");
        assertThat(top.activityReasons()).extracting(CareerPreferenceView.ActivityReason::tag).containsExactly("ai", "project");
        var scored = recommender.scoreAll(roleCatalog.all(player), stats, activities);
        assertThat(rankOf(scored, "AI_NATIVE")).isLessThan(rankOf(scored, "AI"));
    }

    @Test
    @DisplayName("기존에 AI 코드로 저장된 회차는 같은 코드로 조회되고 새 이름과 설명으로 보인다")
    void legacyAiSelection_isReadWithCurrentLabels() {
        UUID id = fixture.enrolledStudent();
        Recommendation stored = new Recommendation(1, "AI", "AI 개발", new BigDecimal("30.00"), List.of(), List.of());
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "initialCareerCode", "AI");
            player.changeCareer("AI");
            player.storeRecommendation(objectMapper.writeValueAsString(new CareerPreferenceRecord(
                    Map.of("I", 40), List.of(stored), new ArrayList<>())));
        });

        CareerPreferenceView view = preferenceService.getPreference(id);

        assertThat(view.initialCareerCode()).isEqualTo("AI");
        assertThat(view.currentCareerCode()).isEqualTo("AI");
        assertThat(view.recommendations()).singleElement().satisfies(r -> {
            assertThat(r.roleCode()).isEqualTo("AI");
            assertThat(r.label()).isEqualTo("인공지능 개발자");
            assertThat(r.score()).isEqualByComparingTo("30.00");
            assertThat(r.summary()).isNotBlank();
        });
    }

    @Test
    @DisplayName("AI 네이티브 개발자는 적합도 순위가 낮아도 처음 확정할 수 있다")
    void select_aiNativeFromRecommendation() {
        UUID id = careerPickerWith(Map.of("P", 70, "C", 60), "project");
        CareerPreferenceView view = preferenceService.getPreference(id);
        assertThat(view.recommendations()).extracting(Recommendation::roleCode).contains("AI_NATIVE");

        var selected = preferenceService.select(id, fixture.version(id), UUID.randomUUID().toString(),
                new SelectCareerPreferenceRequest("AI_NATIVE"));

        assertThat(selected.player().phase()).isEqualTo(Phase.SCHOOL.name());
        assertThat(selected.player().initialCareerCode()).isEqualTo("AI_NATIVE");
        assertThat(selected.player().currentCareerCode()).isEqualTo("AI_NATIVE");

        UUID other = careerPickerWith(Map.of("B", 70), "backend");
        CareerPreferenceView lowerRanked = preferenceService.getPreference(other);
        assertThat(lowerRanked.recommendations()).filteredOn(r -> r.roleCode().equals("AI_NATIVE"))
                .singleElement().satisfies(r -> assertThat(r.rank()).isGreaterThan(3));
        var lowerSelected = preferenceService.select(other, fixture.version(other), UUID.randomUUID().toString(),
                new SelectCareerPreferenceRequest("AI_NATIVE"));
        assertThat(lowerSelected.player().initialCareerCode()).isEqualTo("AI_NATIVE");

    }

    @Test
    @DisplayName("재학 중에는 인공지능 개발자에서 AI 네이티브 개발자로 규칙에 맞게 바꿀 수 있다")
    void change_aiToAiNative_followsExistingRules() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "initialCareerCode", "AI");
            player.changeCareer("AI");
            player.storeRecommendation(objectMapper.writeValueAsString(new CareerPreferenceRecord(
                    Map.of("I", 40), List.of(new Recommendation(1, "AI", "AI 개발", BigDecimal.TEN, List.of(), List.of())),
                    new ArrayList<>())));
        });
        // 능력치 점수가 기준(30) 미만이면 최초 추천 밖인 AI 네이티브로 바꿀 수 없다.
        org.assertj.core.api.Assertions.assertThat(preferenceService.getPreference(id).eligibleChangeRoleCodes())
                .doesNotContain("AI_NATIVE");

        fixture.addStats(id, Map.of("P", 60, "C", 60, "I", 30));
        assertThat(preferenceService.getPreference(id).eligibleChangeRoleCodes()).contains("AI_NATIVE");
        var changed = preferenceService.select(id, fixture.version(id), UUID.randomUUID().toString(),
                new SelectCareerPreferenceRequest("AI_NATIVE"));

        assertThat(changed.player().initialCareerCode()).isEqualTo("AI");
        assertThat(changed.player().currentCareerCode()).isEqualTo("AI_NATIVE");
    }

    @Test
    @DisplayName("AI 네이티브 직무 채용은 같은 직무 작품에 가중치를 주고 오퍼를 받으면 졸업 요약에 남는다")
    void recruitment_aiNativeCompany_toGraduationSummary() {
        UUID id = applicant();
        registeredProject(id, "AI_TOOL_SERVICE", 90);
        fixture.addStats(id, Map.of("P", 60, "B", 60, "C", 60, "A", 60, "F", 40, "I", 60));
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "initialCareerCode", "AI_NATIVE");
            player.changeCareer("AI_NATIVE");
        });

        ApplicationMutation applied = careerService.apply(id, fixture.version(id), UUID.randomUUID().toString(),
                new ApplyJobRequest("AI_WORKS"));
        assertThat(applied.application().status()).isEqualTo("DOC_PASSED");
        assertThat(applied.application().role()).isEqualTo("AI_NATIVE");
        // 같은 직무 작품 90 × 1.0 ÷ 2 = 45
        assertThat(applied.application().p()).isEqualByComparingTo("45");
        UUID applicationId = applied.application().id();
        assertThat(step(id, applicationId, "CODING").application().status()).isEqualTo("CODE_PASSED");
        fixture.modify(id, player -> {
            while (player.getTurn() < 5) player.nextTurn(SchoolRules.AP_PER_TURN);
        });
        UUID questionId = interviewService.getCompanyQuestion(id, applicationId).questionId();
        careerService.proceedStep(id, applicationId, fixture.version(id), UUID.randomUUID().toString(),
                new JobStepRequest("INTERVIEW", questionId, LONG_ANSWER));
        assertThat(step(id, applicationId, "RESULT").application().status()).isEqualTo("OFFERED");

        fixture.modify(id, Player::readyToGraduate);
        EndingMutation ending = careerService.graduate(id, fixture.version(id), UUID.randomUUID().toString(),
                new FinalizeGraduationRequest("EMPLOYMENT", applicationId));

        assertThat(ending.ending().endingCode()).isEqualTo("EMPLOYED");
        assertThat(ending.ending().acceptedRole()).isEqualTo("AI_NATIVE");
        assertThat(ending.ending().summary()).containsEntry("currentCareerCode", "AI_NATIVE")
                .containsEntry("aptitudeMismatch", false);
    }

    @Test
    @DisplayName("인공지능 개발자를 희망하면서 AI 네이티브 직무 오퍼를 수락하면 별도 직무라 적성 불일치로 기록한다")
    void graduation_aiWishWithAiNativeOffer_isMismatch() {
        UUID id = applicant();
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "initialCareerCode", "AI");
            player.changeCareer("AI");
            ReflectionTestUtils.setField(player, "phase", Phase.GRAD_READY);
        });
        UUID offerId = saveOffer(id, "AI_WORKS", "AI_NATIVE");

        EndingMutation ending = careerService.graduate(id, fixture.version(id), UUID.randomUUID().toString(),
                new FinalizeGraduationRequest("EMPLOYMENT", offerId));

        assertThat(ending.ending().endingCode()).isEqualTo("EMPLOYED_MISMATCH");
        assertThat(ending.ending().summary()).containsEntry("currentCareerCode", "AI");
    }

    private UUID saveOffer(UUID playerId, String company, String role) {
        var application = com.silvally.entity.JobApplication.apply(playerId, company, role, 49, 45, 40, true);
        ReflectionTestUtils.setField(application, "status", com.silvally.entity.ApplicationStatus.OFFERED);
        return jobApplications.save(application).getId();
    }

    @Autowired
    private com.silvally.repository.JobApplicationRepository jobApplications;

    private ApplicationMutation step(UUID id, UUID applicationId, String step) {
        return careerService.proceedStep(id, applicationId, fixture.version(id), UUID.randomUUID().toString(),
                new JobStepRequest(step));
    }

    /** 3학년 1턴 학생. */
    private UUID applicant() {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> {
            player.nextYear(SchoolRules.AP_PER_TURN);
            player.nextYear(SchoolRules.AP_PER_TURN);
            player.chooseSemesterFocus("P");
        });
        return id;
    }

    private void registeredProject(UUID id, String projectCode, int quality) {
        PlayerProject project = PlayerProject.start(id, projectCode, 3, 49, 58);
        project.finish(quality, true);
        project.registerPortfolio();
        projectRepository.save(project);
    }

    private UUID careerPickerWith(Map<String, Integer> boost, String tag) {
        UUID id = fixture.enrolledStudent();
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "phase", Phase.CAREER_PICK);
            player.getStats().apply(boost);
        });
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "beforeSchoolDay", 4);
            activityRecords.save(new com.silvally.entity.ActivityRecord(player, "BS04", "DEVELOP", "COMPLETED",
                    "{\"tags\": [\"" + tag + "\", \"ai\"]}"));
            ReflectionTestUtils.setField(player, "beforeSchoolDay", null);
        });
        return id;
    }

    @Autowired
    private com.silvally.repository.ActivityRecordRepository activityRecords;

    private static CareerRecommender.Activity activity(int day, String event, String choice, String label, String... tags) {
        return new CareerRecommender.Activity(day, event, choice, label, new HashSet<>(Set.of(tags)));
    }

    private static int rankOf(List<CareerRecommender.Scored> scored, String code) {
        for (int i = 0; i < scored.size(); i++) {
            if (scored.get(i).role().code().equals(code)) return i;
        }
        throw new AssertionError(code);
    }
}
