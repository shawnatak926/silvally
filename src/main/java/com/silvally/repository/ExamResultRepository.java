package com.silvally.repository;

import com.silvally.entity.ExamKind;
import com.silvally.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    boolean existsByPlayerIdAndYearAndSemesterAndKind(UUID playerId, int year, int semester, ExamKind kind);

    List<ExamResult> findByPlayerIdOrderByIdAsc(UUID playerId);

    void deleteByPlayerId(UUID playerId);
}
