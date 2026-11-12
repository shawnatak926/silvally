package com.silvally.repository;
import com.silvally.entity.EventCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EventCommandRepository extends JpaRepository<EventCommand, Long> {
    Optional<EventCommand> findByPlayerIdAndRequestKey(UUID playerId, String requestKey);

    void deleteByPlayerId(UUID playerId);
}
