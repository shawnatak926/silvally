package com.silvally.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolEvidenceSelectorTest {

    private final ObjectMapper json = JsonMapper.builder().build();
    private final SchoolEvidenceSelector selector;

    SchoolEvidenceSelectorTest() throws IOException {
        selector = new SchoolEvidenceSelector(new SchoolLifeContent(json), json);
    }

    @Test
    void siliconValleyApplicationCarriesSelectionGuideAndSixVersusEightWeekConflict() {
        var evidence = selector.select("J01", "SILICON_VALLEY", "{}");

        assertThat(codes(evidence)).containsExactly("GLOBAL_SELECTION", "GLOBAL_PERIOD_NOTE");
        var conflict = evidence.get(1);
        assertThat(conflict.classification()).isEqualTo("CONFLICT");
        assertThat(conflict.fact()).contains("8주", "6주");
        assertThat(conflict.url()).isEqualTo("/school-sources/school-web-2027.pdf#page=4");
        assertThat(conflict.sourceYear()).isEqualTo(2027);
        assertThat(conflict.gameAdaptation()).contains("게임용 가상 규칙");
    }

    @Test
    void domesticApplicationDoesNotBorrowOverseasDispatchEvidence() {
        assertThat(codes(selector.select("J01", "DOMESTIC", "{}")))
                .containsExactly("CAREER_SUPPORT")
                .doesNotContain("GLOBAL_SELECTION", "GLOBAL_PERIOD_NOTE");
    }

    @Test
    void campusTourMarksDormitoryExtensionAsPlanNotCompletion() {
        var evidence = selector.select("BS01", "DORMITORY", "{}");

        assertThat(codes(evidence)).containsExactly("INFRASTRUCTURE", "REBUILD_PLAN", "DORM");
        var plan = evidence.get(1);
        assertThat(plan.classification()).isEqualTo("PLAN");
        assertThat(plan.appliesTo()).contains("예정", "완료 미확인");
        assertThat(plan.fact()).contains("2026년 12월 입주 예정");
    }

    @Test
    void recordedSceneSourceComesFirstAndNeurowExplainsStrategicThinkingIsNotStress() {
        String recorded = "{\"title\":\"오늘의 회고\",\"source\":{\"code\":\"NEUROW\",\"fact\":\"요약본\"}}";

        var evidence = selector.select("DAILY_REFLECTION", "REFLECT_NOTE", recorded);

        assertThat(codes(evidence)).containsExactly("NEUROW");
        assertThat(evidence.getFirst().fact()).contains("전략적 사고", "스트레스 능력치 S와 다른 의미");
    }

    @Test
    void differentYearsAndEventsReceiveDifferentEvidence() {
        assertThat(codes(selector.select("FOCUS_Y1_S1", "PROGRAMMING", "{}"))).containsExactly("CURRICULUM");
        assertThat(codes(selector.select("NARSHA_RANK", "CHECK", "{}"))).containsExactly("NARSHA");
        assertThat(codes(selector.select("VACATION_Y2_SUMMER", "CAMP", "{}"))).containsExactly("VACATION_CAMP");
        assertThat(selector.select("FOCUS_Y1_S1", "PROGRAMMING", "{}").getFirst().appliesTo())
                .isEqualTo("2026학년도 입학생 기준");
    }

    @Test
    void unrelatedOrUnreadableScenesGetNoEvidenceAndResultsStayBounded() {
        assertThat(selector.select("AD01", "GENERAL", "{}")).isEmpty();
        assertThat(selector.select("X01", "PICK", "not json")).isEmpty();
        assertThat(selector.select(null, null, null)).isEmpty();
        assertThat(selector.select("STRESS_SUPPORT", "TALK",
                "{\"source\":{\"code\":\"UNKNOWN_SOURCE\"}}")).extracting(NarrativeContext.SchoolEvidence::code)
                .containsExactly("NEUROW", "DORM", "REBUILD_PLAN");
        for (String scene : List.of("BS01", "BS02", "BS05", "SOFTWAREWAVE", "WITHDRAWAL_DECISION")) {
            assertThat(selector.select(scene, "ANY", "{}")).hasSizeLessThanOrEqualTo(SchoolEvidenceSelector.MAX_ITEMS)
                    .allSatisfy(e -> assertThat(e.fact().length()).isLessThanOrEqualTo(SchoolEvidenceSelector.MAX_FACT_LENGTH));
        }
    }

    private static List<String> codes(List<NarrativeContext.SchoolEvidence> evidence) {
        return evidence.stream().map(NarrativeContext.SchoolEvidence::code).toList();
    }
}
