package com.silvally.repository;

import com.silvally.entity.CommandLog;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {
    List<CommandLog> findByPlayerIdAndIdGreaterThanOrderByIdAsc(UUID playerId, Long afterId, Pageable pageable);

    List<CommandLog> findByPlayerIdAndSubjectCodeOrderByIdAsc(UUID playerId, String subjectCode);

    Optional<CommandLog> findByPlayerIdAndCommandId(UUID playerId, UUID commandId);

    @Query("""
            select new com.silvally.repository.HistoryRow(
                c.id, c.year, c.turn, c.beforeSchoolDay, c.kind, c.subjectCode, c.choiceCode, c.resultCode,
                n.status, n.text)
            from CommandLog c
            left join LifeNarrative n on n.commandLogId = c.id
            where c.playerId = :playerId and c.id > :afterId
            order by c.id asc
            """)
    List<HistoryRow> findHistory(@Param("playerId") UUID playerId, @Param("afterId") long afterId, Limit limit);

    void deleteByPlayerId(UUID playerId);
}
