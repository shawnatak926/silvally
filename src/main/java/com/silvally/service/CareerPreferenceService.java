package com.silvally.service;

import com.silvally.dto.request.SelectCareerPreferenceRequest;
import com.silvally.dto.response.CareerPreferenceMutation;
import com.silvally.dto.response.CareerPreferenceView;

import java.util.UUID;

public interface CareerPreferenceService {
    CareerPreferenceView getPreference(UUID playerId);

    CareerPreferenceMutation select(UUID playerId, long expectedVersion, String idempotencyKey,
                                    SelectCareerPreferenceRequest request);
}
