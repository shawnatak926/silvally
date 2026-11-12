package com.silvally.repository;
import com.silvally.entity.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {
    List<ActivityRecord> findByPlayerIdAndEventCodeOrderByIdAsc(UUID playerId, String eventCode);
    List<ActivityRecord> findByPlayerIdOrderByIdAsc(UUID playerId);
}
