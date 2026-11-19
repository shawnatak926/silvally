package com.silvally.repository;

import com.silvally.entity.LifeNarrative;
import com.silvally.entity.NarrativeSource;
import com.silvally.entity.NarrativeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LifeNarrativeRepository extends JpaRepository<LifeNarrative, Long> {

    Optional<LifeNarrative> findByCommandLogId(Long commandLogId);

    List<LifeNarrative> findTop3ByPlayerIdAndStatusAndCommandLogIdLessThanOrderByCommandLogIdDesc(
            UUID playerId, NarrativeStatus status, Long commandLogId);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update LifeNarrative n
            set n.status = com.silvally.entity.NarrativeStatus.SAVED, n.text = :text,
                n.source = :source, n.model = :model,
                n.attempts = n.attempts + 1, n.lastError = null
            where n.id = :id and n.status <> com.silvally.entity.NarrativeStatus.SAVED
            """)
    int markSaved(@Param("id") Long id, @Param("text") String text, @Param("source") NarrativeSource source,
                  @Param("model") String model);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update LifeNarrative n
            set n.status = com.silvally.entity.NarrativeStatus.FAILED, n.text = :fallbackText,
                n.source = com.silvally.entity.NarrativeSource.FALLBACK, n.model = null,
                n.attempts = n.attempts + 1, n.lastError = :error
            where n.id = :id and n.status <> com.silvally.entity.NarrativeStatus.SAVED
            """)
    int markFailed(@Param("id") Long id, @Param("error") String error, @Param("fallbackText") String fallbackText);

    @Query("""
            select n.id from LifeNarrative n
            where n.playerId = :playerId
              and n.status <> com.silvally.entity.NarrativeStatus.SAVED
              and n.attempts < :maxAttempts
            order by n.id asc
            """)
    List<Long> findRetryTargets(@Param("playerId") UUID playerId, @Param("maxAttempts") int maxAttempts);

    void deleteByPlayerId(UUID playerId);
}
