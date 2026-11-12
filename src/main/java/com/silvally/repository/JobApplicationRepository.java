package com.silvally.repository;

import com.silvally.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByPlayerIdOrderByCompanyCodeAsc(UUID playerId);

    void deleteByPlayerId(UUID playerId);
}
