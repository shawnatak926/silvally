package com.silvally.service;

public class NarrativeGenerationException extends RuntimeException {
    public NarrativeGenerationException(String message) {
        super(message);
    }

    public NarrativeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
