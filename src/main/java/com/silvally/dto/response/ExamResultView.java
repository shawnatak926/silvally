package com.silvally.dto.response;

import com.silvally.entity.ExamResult;

public record ExamResultView(int year, int semester, String kind, int turn, int preparation, int score,
                             int gradeBefore, int gradeAfter, String ruleSource) {

    public static ExamResultView of(ExamResult result, String ruleSource) {
        return new ExamResultView(result.getYear(), result.getSemester(), result.getKind().name(), result.getTurn(),
                result.getPreparation(), result.getScore(), result.getGradeBefore(), result.getGradeAfter(), ruleSource);
    }
}
