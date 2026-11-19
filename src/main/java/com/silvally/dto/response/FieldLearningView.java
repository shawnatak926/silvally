package com.silvally.dto.response;

public record FieldLearningView(String path, String status, Integer englishAtApplication, String failureReason,
                                boolean selected, boolean prepared, boolean completed,
                                Integer appliedYear, Integer appliedTurn, Integer preparedTurn, Integer completedTurn) { }
