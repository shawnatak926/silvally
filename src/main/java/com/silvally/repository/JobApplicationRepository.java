package com.silvally.repository;

import com.silvally.entity.ApplicationStatus;
import com.silvally.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByPlayerIdOrderByCompanyCodeAsc(UUID playerId);

    Optional<JobApplication> findByIdAndPlayerId(UUID id, UUID playerId);

    boolean existsByPlayerIdAndCompanyCode(UUID playerId, String companyCode);

    List<JobApplication> findByPlayerIdAndStatusIn(UUID playerId, Collection<ApplicationStatus> statuses);

    void deleteByPlayerId(UUID playerId);
}
