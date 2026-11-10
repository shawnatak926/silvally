package com.silvally.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdmissionPolicyTest {

    private final AdmissionPolicy policy = new AdmissionPolicy();

    @Test
    @DisplayName("일반전형은 모든 배경이 지원할 수 있다")
    void generalTrackAcceptsEveryProfile() {
        for (String profile : new String[]{"OPEN", "MAKER", "REGION", "ACCESS"}) {
            assertThat(policy.isEligible(profile, "GENERAL")).isTrue();
        }
    }

    @Test
    @DisplayName("특별전형은 해당 배경만 지원할 수 있다")
    void specialTrackRequiresMatchingProfile() {
        assertThat(policy.isEligible("MAKER", "MEISTER")).isTrue();
        assertThat(policy.isEligible("REGION", "LOCAL")).isTrue();
        assertThat(policy.isEligible("ACCESS", "SOCIAL")).isTrue();
    }

    @Test
    @DisplayName("자격이 없는 특별전형 지원은 거부된다")
    void specialTrackRejectsOtherProfile() {
        assertThat(policy.isEligible("OPEN", "MEISTER")).isFalse();
        assertThat(policy.isEligible("MAKER", "LOCAL")).isFalse();
        assertThat(policy.isEligible("OPEN", "UNKNOWN")).isFalse();
    }
}
