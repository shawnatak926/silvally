package com.silvally.service;

import com.silvally.dto.response.CourseView;
import com.silvally.dto.response.MutationResult;
import com.silvally.entity.Player;

import java.util.List;
import java.util.UUID;

public interface CertificateService {
    List<CourseView> getCourses(UUID playerId);

    MutationResult enroll(UUID playerId, String courseCode, long expectedVersion, String idempotencyKey);

    MutationResult attend(UUID playerId, String courseCode, long expectedVersion, String idempotencyKey);

    MutationResult takeExam(UUID playerId, String courseCode, long expectedVersion, String idempotencyKey);

    List<String> expireAtYearEnd(Player player, int nextYear);
}
