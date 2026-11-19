package com.silvally.dto.response;

import java.util.UUID;

public record InterviewView(UUID questionId, String question, String source, String kind) { }
