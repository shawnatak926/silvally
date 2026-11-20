package com.silvally.repository;

import com.silvally.entity.NarshaProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NarshaProjectRepository extends JpaRepository<NarshaProject, UUID> {

    List<NarshaProject> findByPlayerIdOrderByYearAsc(UUID playerId);

    Optional<NarshaProject> findByPlayerIdAndYear(UUID playerId, int year);

    void deleteByPlayerId(UUID playerId);
}
