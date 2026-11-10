package com.silvally.repository;

import com.silvally.entity.EventDef;
import com.silvally.entity.Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventDefRepository extends JpaRepository<EventDef, EventDef.Id> {
    @Query("""
            select e from EventDef e
            where e.id.rulesVersion = :rulesVersion and e.phase = :phase and e.year = :year
            order by e.orderNo asc, e.id.eventCode asc
            """)
    List<EventDef> findEvents(@Param("rulesVersion") String rulesVersion,
                              @Param("phase") Phase phase,
                              @Param("year") int year);
}
