package com.silvally.repository;

import com.silvally.entity.PlayerProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerProjectRepository extends JpaRepository<PlayerProject, UUID> {

    List<PlayerProject> findByPlayerId(UUID playerId);
}
