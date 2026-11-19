package com.silvally.service;

import com.silvally.entity.CommandLog;

public interface NarrativeService {
    void request(CommandLog sceneLog);

    void write(Long narrativeId);
}
