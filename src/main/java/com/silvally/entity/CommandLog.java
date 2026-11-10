package com.silvally.entity;

import com.silvally.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "command_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "command_id"}),
        indexes = @Index(name = "idx_command_log_player",
                columnList = "player_id, school_year, school_turn, kind, id"))
public class CommandLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private UUID commandId;

    @Column(name = "school_year", nullable = false)
    private int year;

    @Column(name = "school_turn", nullable = false)
    private int turn;

    private Integer beforeSchoolDay;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String subjectCode;

    private String choiceCode;

    private String invokedActionCode;

    @Lob
    private String evidenceJson;

    @Column(nullable = false)
    private String requestHash;

    @Column(nullable = false)
    private int httpStatus;

    @Lob
    private String responseJson;

    @Lob
    private String afterStatsJson;

    protected CommandLog() {
    }

    public Long getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getCommandId() {
        return commandId;
    }
}
