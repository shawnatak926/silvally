package com.silvally.repository;

import com.silvally.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {
    Optional<InterviewQuestion> findByPlayerIdAndKindAndSubjectCode(UUID playerId, InterviewKind kind, String subjectCode);
    void deleteByPlayerId(UUID playerId);
}
