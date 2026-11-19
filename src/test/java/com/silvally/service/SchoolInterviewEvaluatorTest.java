package com.silvally.service;

import com.silvally.support.AdmissionAnswers;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SchoolInterviewEvaluatorTest {
    final SchoolInterviewEvaluator evaluator = new SchoolInterviewEvaluator();
    @Test void repeatedKeywordsCannotStandInForConcreteExperience() {
        var result = evaluator.evaluate("지원 동기 관심 활동 프로젝트 경험 문제 해결 오류 입학 계획 학습. ".repeat(8));
        assertThat(result.score()).isLessThanOrEqualTo(10);
    }
    @Test void concreteRoleCauseFixVerificationAndTimedPlanAreRecognized() {
        var result = evaluator.evaluate(AdmissionAnswers.STRONG);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.components()).containsEntry("motivation", 25).containsEntry("experience", 25)
                .containsEntry("problemSolving", 25).containsEntry("schoolPlan", 25);
    }
    @Test void genericPromisesAreWeakerThanEvidence() {
        assertThat(evaluator.evaluate("지원하고 싶습니다. 프로젝트 경험이 있습니다. 문제를 해결했습니다. 입학 후 공부하겠습니다.").score())
                .isLessThan(40);
    }
    @Test void explicitAbsenceOfExperienceIsNotAwardedExperiencePoints() {
        assertThat(evaluator.evaluate("프로젝트 경험이 없습니다. 문제를 해결한 적이 없고 학습 계획이 없습니다.").score()).isZero();
    }
    @Test void eachCriterionStoresQuotedEvidenceAndItsOwnReasonsWithoutFrequencyBonus() {
        var result = evaluator.evaluate(AdmissionAnswers.STRONG);
        assertThat(result.rubricVersion()).isEqualTo("school-evidence-v1");
        assertThat(result.criteria()).hasSize(4).allSatisfy((key, criterion) -> {
            assertThat(criterion.maxScore()).isEqualTo(25);
            assertThat(criterion.evidence()).isNotEmpty().allSatisfy(quote -> assertThat(AdmissionAnswers.STRONG).contains(quote));
            assertThat(criterion.reasons()).hasSize(5);
        });
        assertThat(result.criteria().get("problemSolving").evidence()).anyMatch(text -> text.contains("멱등키"));
        assertThat(evaluator.evaluate(AdmissionAnswers.STRONG.repeat(2)).score()).isEqualTo(result.score());
    }
}
