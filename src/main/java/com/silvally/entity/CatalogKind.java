package com.silvally.entity;

import java.util.Arrays;
import java.util.Optional;

public enum CatalogKind {
    ACTION("actions"),
    ITEM("items"),
    COURSE("courses"),
    PROJECT("projects"),
    COMPANY("companies"),
    ENDING("endings"),
    ADMISSION_TRACK("admission-tracks"),
    CAREER_ROLE("career-roles");

    private final String path;

    CatalogKind(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static Optional<CatalogKind> fromPath(String path) {
        return Arrays.stream(values()).filter(k -> k.path.equals(path)).findFirst();
    }
}
