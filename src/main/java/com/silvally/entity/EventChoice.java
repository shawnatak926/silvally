package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "event_choice")
public class EventChoice {
    @Embeddable
    public record Id(String rulesVersion, String eventCode, String choiceCode) implements Serializable {
    }

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int apCost;

    private String effectCode;

    @Lob
    private String effectJson;

    @Column(nullable = false)
    private boolean heavy;

    private String followUpHint;

    protected EventChoice() {
    }

    public Id getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getApCost() {
        return apCost;
    }
}
