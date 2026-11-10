package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "event_def",
        indexes = @Index(name = "idx_event_def_lookup", columnList = "rules_version, phase, school_year, min_turn"))
public class EventDef {
    @Embeddable
    public record Id(String rulesVersion, String eventCode) implements Serializable {
    }

    @EmbeddedId
    private Id id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Phase phase;

    @Column(name = "school_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private int minTurn;

    @Column(nullable = false)
    private int maxTurn;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private int orderNo;

    @Lob
    private String triggerJson;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String situation;

    protected EventDef() {
    }

    public Id getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSituation() {
        return situation;
    }

    public boolean isRequired() {
        return required;
    }
}
