package com.silvally.service;

import com.silvally.dto.response.InterviewView;
import com.silvally.entity.*;
import java.util.UUID;

/** 회사 면접 채점·전형 상태 변경은 호출하는 채용 서비스가 담당합니다. */
public interface InterviewService {
    com.silvally.dto.response.InterviewValidationResult submitAnswer(Player player, InterviewKind kind,
            String subjectCode, UUID questionId, String answer);
    InterviewView getSchoolQuestion(UUID playerId);
    InterviewView getCompanyQuestion(UUID playerId, UUID applicationId);
    String validateAnswer(Player player, InterviewKind kind, String subjectCode, UUID questionId, String answer);
}
