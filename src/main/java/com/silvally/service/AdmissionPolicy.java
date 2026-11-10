package com.silvally.service;

import org.springframework.stereotype.Component;

@Component
public class AdmissionPolicy {
    public boolean isEligible(String applicationProfile, String admissionTrack) {
        return switch (admissionTrack) {
            case "GENERAL" -> true;
            case "MEISTER" -> "MAKER".equals(applicationProfile);
            case "LOCAL" -> "REGION".equals(applicationProfile);
            case "SOCIAL" -> "ACCESS".equals(applicationProfile);
            default -> false;
        };
    }
}
