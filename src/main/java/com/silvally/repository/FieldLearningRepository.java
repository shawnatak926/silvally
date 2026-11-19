package com.silvally.repository;

import com.silvally.entity.FieldLearning;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FieldLearningRepository extends JpaRepository<FieldLearning, UUID> { }
