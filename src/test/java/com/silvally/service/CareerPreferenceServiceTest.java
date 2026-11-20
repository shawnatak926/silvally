package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.CreatePlayerRequest;
import com.silvally.dto.request.SelectCareerPreferenceRequest;
import com.silvally.dto.response.CareerPreferenceMutation;
import com.silvally.dto.response.CareerPreferenceView;
import com.silvally.entity.ActivityRecord;
import com.silvally.entity.Phase;
import com.silvally.entity.Player;
import com.silvally.repository.ActivityRecordRepository;
import com.silvally.repository.PlayerRepository;
import com.silvally.support.TestStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CareerPreferenceServiceTest {

    @Autowired
    private CareerPreferenceService careerPreferenceService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Autowired
    private SchoolFixture fixture;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("비포스쿨 활동과 능력치로 전체 11개 직무를 적합도와 이유 순으로 돌려준다")
    void getPreference_allElevenWithReasons() {
        UUID id = careerPicker();

        CareerPreferenceView view = careerPreferenceService.getPreference(id);

        assertThat(view.recommendations()).extracting(CareerPreferenceView.Recommendation::roleCode)
                .startsWith("WEB", "CROSS_PLATFORM", "DESIGN");
        assertThat(view.recommendations()).hasSize(11);
        assertThat(view.recommendations()).extracting(CareerPreferenceView.Recommendation::roleCode).doesNotHaveDuplicates();
        assertThat(view.recommendations()).extracting(CareerPreferenceView.Recommendation::score).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        CareerPreferenceView.Recommendation web = view.recommendations().get(0);
        // F 38, P 20 → (3×38 + 1×20) ÷ 4 = 33.5, 웹 태그 활동 2건 × 4점
        assertThat(web.score()).isEqualByComparingTo("41.50");
        assertThat(web.statReasons()).extracting(CareerPreferenceView.StatReason::statCode).containsExactly("F", "P");
        assertThat(web.activityReasons()).extracting(CareerPreferenceView.ActivityReason::choiceCode)
                .containsExactly("WEB", "WEB_MOBILE");
        assertThat(web.activityReasons().get(0).activityLabel()).isEqualTo("웹 개발");
        assertThat(view.initialCareerCode()).isNull();
        assertThat(view.canChange()).isFalse();
    }

    @Test
    @DisplayName("추천 조회는 회차 버전을 올리지 않고, 저장된 최초 추천은 능력치가 변해도 유지된다")
    void getPreference_storedOnce_andVersionUnchanged() {
        UUID id = careerPicker();
        long version = fixture.version(id);

        CareerPreferenceView first = careerPreferenceService.getPreference(id);
        fixture.addStats(id, Map.of("I", 60, "A", 60));
        long changedVersion = fixture.version(id);
        CareerPreferenceView second = careerPreferenceService.getPreference(id);

        assertThat(version).isLessThanOrEqualTo(changedVersion);
        assertThat(fixture.version(id)).isEqualTo(changedVersion);
        assertThat(second.recommendations()).isEqualTo(first.recommendations());
        assertThat(playerRepository.findById(id).orElseThrow().getRecommendationJson()).isNotNull();
    }

    @Test
    @DisplayName("비포스쿨을 마치기 전에는 추천을 조회할 수 없다")
    void getPreference_beforeCareerPick_wrongPhase() {
        UUID id = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("student", null)).id();

        assertError(() -> careerPreferenceService.getPreference(id), ErrorCode.WRONG_PHASE);
    }

    @Test
    @DisplayName("목록의 희망 직무를 확정하면 재학이 시작되고 최초 직무가 남는다")
    void select_recommendedRole_startsSchool() {
        UUID id = careerPicker();

        CareerPreferenceMutation result = select(id, "WEB");

        assertThat(result.player().phase()).isEqualTo(Phase.SCHOOL.name());
        assertThat(result.player().initialCareerCode()).isEqualTo("WEB");
        assertThat(result.player().currentCareerCode()).isEqualTo("WEB");
        assertThat(result.preference().initialCareerCode()).isEqualTo("WEB");
        assertThat(history(id)).singleElement().satisfies(change -> {
            assertThat(change.type()).isEqualTo("SELECT");
            assertThat(change.fromRole()).isNull();
            assertThat(change.toRole()).isEqualTo("WEB");
        });
    }

    @Test
    @DisplayName("상위 3개 밖 최하위 직무도 선택할 수 있고 알 수 없는 직무는 거절한다")
    void select_lowestRankedRole_allowedAndUnknownRejected() {
        UUID id = careerPicker();
        CareerPreferenceView view = careerPreferenceService.getPreference(id);
        String lowest = view.recommendations().getLast().roleCode();
        assertError(() -> select(id, "ASTRONAUT"), ErrorCode.VALIDATION_ERROR);
        CareerPreferenceMutation result = select(id, lowest);
        assertThat(result.player().initialCareerCode()).isEqualTo(lowest);
        assertThat(result.player().currentCareerCode()).isEqualTo(lowest);
        assertThat(result.player().phase()).isEqualTo(Phase.SCHOOL.name());
    }

    @Test
    @DisplayName("상위 3개만 저장된 회차는 원래 능력치와 이력을 보존하며 전체 직무를 보여준다")
    void legacyRecommendations_expandWithoutChangingVersionOrInitialStats() {
        UUID id = careerPicker();
        CareerPreferenceView first = careerPreferenceService.getPreference(id);
        fixture.modify(id, p -> {
            CareerPreferenceRecord saved = objectMapper.readValue(p.getRecommendationJson(), CareerPreferenceRecord.class);
            p.storeRecommendation(objectMapper.writeValueAsString(new CareerPreferenceRecord(saved.stats(), saved.recommendations().subList(0, 3), saved.history())));
            p.getStats().apply(Map.of("I", 60));
        });
        long version = fixture.version(id);
        CareerPreferenceView expanded = careerPreferenceService.getPreference(id);
        assertThat(expanded.recommendations()).isEqualTo(first.recommendations());
        assertThat(fixture.version(id)).isEqualTo(version);
        select(id, expanded.recommendations().getLast().roleCode());
        assertThat(history(id)).hasSize(1);
    }

    @Test
    @DisplayName("재학 중에는 조건을 만족하는 직무로 최대 2번까지 바꾸고 변경 이력이 남는다")
    void select_changeInSchool_limitedAndRecorded() {
        UUID id = careerPicker();
        select(id, "WEB");

        CareerPreferenceView view = careerPreferenceService.getPreference(id);
        assertThat(view.canChange()).isTrue();
        assertThat(view.eligibleChangeRoleCodes()).containsExactly("CROSS_PLATFORM", "DESIGN");

        assertError(() -> select(id, "AI"), ErrorCode.LOCKED);
        assertError(() -> select(id, "WEB"), ErrorCode.VALIDATION_ERROR);

        CareerPreferenceMutation first = select(id, "DESIGN");
        assertThat(first.player().initialCareerCode()).isEqualTo("WEB");
        assertThat(first.player().currentCareerCode()).isEqualTo("DESIGN");
        select(id, "CROSS_PLATFORM");

        assertError(() -> select(id, "WEB"), ErrorCode.LOCKED);
        CareerPreferenceView after = careerPreferenceService.getPreference(id);
        assertThat(after.canChange()).isFalse();
        assertThat(after.eligibleChangeRoleCodes()).isEmpty();
        assertThat(after.initialCareerCode()).isEqualTo("WEB");
        assertThat(after.currentCareerCode()).isEqualTo("CROSS_PLATFORM");
        assertThat(history(id)).extracting(c -> c.type() + ":" + c.fromRole() + ">" + c.toRole())
                .containsExactly("SELECT:null>WEB", "CHANGE:WEB>DESIGN", "CHANGE:DESIGN>CROSS_PLATFORM");
    }

    @Test
    @DisplayName("비포스쿨 전이나 졸업 단계에서는 희망 직무를 정할 수 없다")
    void select_wrongPhase_rejected() {
        UUID early = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("student", null)).id();
        assertError(() -> select(early, "WEB"), ErrorCode.WRONG_PHASE);

        UUID graduating = careerPicker();
        select(graduating, "WEB");
        fixture.modify(graduating, Player::readyToGraduate);
        assertError(() -> select(graduating, "DESIGN"), ErrorCode.WRONG_PHASE);
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 재시도하면 한 번만 확정한다")
    void select_sameKeyRetry_appliesOnce() {
        UUID id = careerPicker();
        long version = fixture.version(id);
        String key = UUID.randomUUID().toString();
        SelectCareerPreferenceRequest request = new SelectCareerPreferenceRequest("WEB");

        CareerPreferenceMutation first = careerPreferenceService.select(id, version, key, request);
        CareerPreferenceMutation retry = careerPreferenceService.select(id, version, key, request);

        assertThat(retry).isEqualTo(first);
        assertThat(history(id)).hasSize(1);
    }

    /** 비포스쿨을 마친 회차: 웹 관련 활동 2건과 F 능력치를 올려 둔다. */
    private UUID careerPicker() {
        UUID id = playerService.create(UUID.randomUUID().toString(), new CreatePlayerRequest("student", null)).id();
        fixture.modify(id, player -> {
            ReflectionTestUtils.setField(player, "phase", Phase.CAREER_PICK);
            TestStats.pin(player);
            player.getStats().apply(Map.of("F", 30));
            ReflectionTestUtils.setField(player, "beforeSchoolDay", 2);
            activityRecordRepository.save(new ActivityRecord(player, "BS02", "WEB", "COMPLETED",
                    "{\"tags\": [\"web\"]}"));
            ReflectionTestUtils.setField(player, "beforeSchoolDay", 5);
            activityRecordRepository.save(new ActivityRecord(player, "BS05", "WEB_MOBILE", "COMPLETED",
                    "{\"tags\": [\"web\", \"android\", \"ios\"]}"));
            ReflectionTestUtils.setField(player, "beforeSchoolDay", null);
        });
        return id;
    }

    private CareerPreferenceMutation select(UUID id, String roleCode) {
        return careerPreferenceService.select(id, fixture.version(id), UUID.randomUUID().toString(),
                new SelectCareerPreferenceRequest(roleCode));
    }

    private List<CareerPreferenceRecord.Change> history(UUID id) {
        String json = playerRepository.findById(id).orElseThrow().getRecommendationJson();
        return objectMapper.readValue(json, CareerPreferenceRecord.class).history();
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
