package com.silvally.repository;

import com.silvally.entity.CommandLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {
    List<CommandLog> findByPlayerIdAndIdGreaterThanOrderByIdAsc(UUID playerId, Long afterId, Pageable pageable);

    List<CommandLog> findByPlayerIdAndSubjectCodeOrderByIdAsc(UUID playerId, String subjectCode);

    Optional<CommandLog> findByPlayerIdAndCommandId(UUID playerId, UUID commandId);
}
