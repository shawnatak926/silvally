package com.silvally.repository;

import com.silvally.entity.SchoolEventDraw;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SchoolEventDrawRepository extends JpaRepository<SchoolEventDraw, Long> {
    Optional<SchoolEventDraw> findByPlayerIdAndPeriodKeyAndDrawKind(UUID playerId, String periodKey, String drawKind);
    void deleteByPlayerId(UUID playerId);
}
