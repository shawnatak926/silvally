package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.NarshaInput;
import com.silvally.entity.NarshaProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NarshaRulesTest {

    private final NarshaRules rules = new NarshaRules();

    @Test
    @DisplayName("직접 입력 주제는 공백 정리 후 2~80자만 허용한다")
    void normalizeTopic_lengthBoundaries() {
        assertThat(rules.normalizeTopic("가나")).isEqualTo("가나");
        assertThat(rules.normalizeTopic("가".repeat(80))).hasSize(80);
        assertInvalid(null);
        assertInvalid("");
        assertInvalid("   ");
        assertInvalid("가");
        assertInvalid("  가  ");
        assertInvalid("가".repeat(81));
    }

    @Test
    @DisplayName("앞뒤 공백을 지우고 연속 공백과 줄바꿈은 공백 하나로 합친다")
    void normalizeTopic_collapsesWhitespace() {
        assertThat(rules.normalizeTopic("  우리   팀의\n프로젝트\t주제  ")).isEqualTo("우리 팀의 프로젝트 주제");
        // 정리 후 길이로 세므로 공백이 많아도 81자 입력이 80자로 줄면 통과한다.
        assertThat(rules.normalizeTopic("가".repeat(40) + "     " + "나".repeat(39))).hasSize(80);
        assertInvalid("가".repeat(40) + " " + "나".repeat(40));
    }

    @Test
    @DisplayName("보이지 않는 제어 문자가 섞인 주제는 거절하고 이모지 같은 보조 문자는 한 글자로 센다")
    void normalizeTopic_hiddenCharacters() {
        assertInvalid("프로젝트\u0000주제");
        assertInvalid("프로젝트​주제");
        assertThat(rules.normalizeTopic("🚀".repeat(2))).hasSize(4);
        assertThat(rules.normalizeTopic("🚀".repeat(80))).isNotBlank();
        assertInvalid("🚀".repeat(81));
    }

    @Test
    @DisplayName("후보 주제와 직접 입력 중 하나만 받고 역할은 제시된 값만 허용한다")
    void resolve_candidateCustomAndRole() {
        NarshaRules.Resolved candidate = rules.resolve(new NarshaInput("TRAVEL_PLATFORM", null, "BACKEND"));
        assertThat(candidate.source()).isEqualTo(NarshaProject.SOURCE_CANDIDATE);
        assertThat(candidate.topic()).isEqualTo("여행 일정 공유 플랫폼");
        assertThat(candidate.role()).isEqualTo("BACKEND");

        NarshaRules.Resolved custom = rules.resolve(new NarshaInput("CUSTOM", "  학교 급식 알림 앱  ", null));
        assertThat(custom.source()).isEqualTo(NarshaProject.SOURCE_CUSTOM);
        assertThat(custom.topic()).isEqualTo("학교 급식 알림 앱");
        assertThat(custom.role()).isEqualTo(NarshaRules.DEFAULT_ROLE);

        assertInvalidInput(null);
        assertInvalidInput(new NarshaInput(null, "주제", null));
        assertInvalidInput(new NarshaInput(" ", null, null));
        assertInvalidInput(new NarshaInput("CUSTOM", null, null));
        assertInvalidInput(new NarshaInput("NO_SUCH_TOPIC", null, null));
        assertInvalidInput(new NarshaInput("TRAVEL_PLATFORM", "같이 보낸 직접 입력", null));
        assertInvalidInput(new NarshaInput("TRAVEL_PLATFORM", null, "KING"));
    }

    @Test
    @DisplayName("완성도는 팀 능력치에 작업·멘토링 상한까지 더하고 0~100으로 제한한다")
    void quality_capsActivityAndClamps() {
        assertThat(rules.quality(40, 0, 0)).isEqualTo(40);
        assertThat(rules.quality(40, 3, 1)).isEqualTo(40 + 6 + 3);
        assertThat(rules.quality(40, 99, 99)).isEqualTo(40 + 12 + 6);
        assertThat(rules.quality(95, 6, 2)).isEqualTo(100);
        assertThat(rules.quality(0, 0, 0)).isZero();
    }

    @Test
    @DisplayName("순위는 완성도 10점마다 한 계단이고 3위까지만 전시에 나갈 수 있다")
    void rank_boundariesAndExhibit() {
        assertThat(rules.rank(100)).isEqualTo(1);
        assertThat(rules.rank(91)).isEqualTo(1);
        assertThat(rules.rank(90)).isEqualTo(2);
        assertThat(rules.rank(81)).isEqualTo(2);
        assertThat(rules.rank(80)).isEqualTo(3);
        assertThat(rules.rank(71)).isEqualTo(3);
        assertThat(rules.rank(70)).isEqualTo(4);
        assertThat(rules.rank(0)).isEqualTo(11);
        assertThat(rules.canExhibit(3)).isTrue();
        assertThat(rules.canExhibit(4)).isFalse();
        assertThat(rules.canExhibit(null)).isFalse();
    }

    private void assertInvalid(String topic) {
        assertThatThrownBy(() -> rules.normalizeTopic(topic)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    private void assertInvalidInput(NarshaInput input) {
        assertThatThrownBy(() -> rules.resolve(input)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
