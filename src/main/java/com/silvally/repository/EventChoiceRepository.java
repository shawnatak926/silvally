package com.silvally.repository;

import com.silvally.entity.EventChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventChoiceRepository extends JpaRepository<EventChoice, EventChoice.Id> {
    @Query("""
            select c from EventChoice c
            where c.id.rulesVersion = :rulesVersion and c.id.eventCode in :eventCodes
            order by c.id.eventCode asc, c.id.choiceCode asc
            """)
    List<EventChoice> findByEventCodes(@Param("rulesVersion") String rulesVersion,
                                       @Param("eventCodes") Collection<String> eventCodes);
}
