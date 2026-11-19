package com.silvally.repository;

import com.silvally.entity.LifeNarrative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LifeNarrativeRepository extends JpaRepository<LifeNarrative, Long> {

    Optional<LifeNarrative> findByCommandLogId(Long commandLogId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update LifeNarrative n
            set n.status = com.silvally.entity.NarrativeStatus.SAVED, n.text = :text,
                n.attempts = n.attempts + 1, n.lastError = null
            where n.id = :id and n.status <> com.silvally.entity.NarrativeStatus.SAVED
            """)
    int markSaved(@Param("id") Long id, @Param("text") String text);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update LifeNarrative n
            set n.status = com.silvally.entity.NarrativeStatus.FAILED,
                n.attempts = n.attempts + 1, n.lastError = :error
            where n.id = :id and n.status <> com.silvally.entity.NarrativeStatus.SAVED
            """)
    int markFailed(@Param("id") Long id, @Param("error") String error);

    void deleteByPlayerId(UUID playerId);
}
