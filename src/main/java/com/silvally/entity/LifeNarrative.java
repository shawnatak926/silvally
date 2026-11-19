package com.silvally.entity;

import com.silvally.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "life_narrative",
        indexes = @Index(name = "idx_life_narrative_status", columnList = "status, attempts"))
public class LifeNarrative extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false, unique = true)
    private Long commandLogId;

    @Column(nullable = false)
    private String sceneId;

    private String choiceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NarrativeStatus status;

    @Lob
    private String text;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 500)
    private String lastError;

    protected LifeNarrative() {
    }

    public static LifeNarrative pending(CommandLog log) {
        LifeNarrative narrative = new LifeNarrative();
        narrative.playerId = log.getPlayerId();
        narrative.commandLogId = log.getId();
        narrative.sceneId = log.getSubjectCode();
        narrative.choiceCode = log.getChoiceCode();
        narrative.status = NarrativeStatus.PENDING;
        return narrative;
    }

    public Long getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public Long getCommandLogId() { return commandLogId; }
    public String getSceneId() { return sceneId; }
    public String getChoiceCode() { return choiceCode; }
    public NarrativeStatus getStatus() { return status; }
    public String getText() { return text; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
}
