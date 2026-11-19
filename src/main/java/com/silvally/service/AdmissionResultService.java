package com.silvally.service;

import com.silvally.dto.response.AdmissionResultView;
import java.util.UUID;

public interface AdmissionResultService {
    AdmissionResultView get(UUID playerId, String eventCode);
}
