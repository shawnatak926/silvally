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

    private String resultCode;

    protected CommandLog() {
    }

    public static CommandLog activity(Player player, String event, String choice, String evidence) {
        return scene(player, UUID.randomUUID(), event, choice, "COMPLETED", evidence);
    }

    public static CommandLog scene(Player player, UUID commandId, String sceneId, String choice,
                                   String resultCode, String evidence) {
        CommandLog log = new CommandLog();
        log.playerId = player.getId();
        log.commandId = commandId;
        log.year = player.getYear();
        log.turn = player.getTurn();
        log.beforeSchoolDay = player.getBeforeSchoolDay();
        log.kind = "EVENT";
        log.subjectCode = sceneId;
        log.choiceCode = choice;
        log.evidenceJson = evidence;
        log.resultCode = resultCode;
        log.requestHash = commandId.toString();
        log.httpStatus = 200;
        return log;
    }
    public void recordActivity(String choiceCode, String evidenceJson) {
        this.choiceCode = choiceCode;
        this.evidenceJson = evidenceJson;
    }
    public String getChoiceCode() { return choiceCode; }
    public String getSubjectCode() { return subjectCode; }
    public int getYear() { return year; }
    public int getTurn() { return turn; }
    public String getResultCode() { return resultCode; }
    public String getEvidenceJson() { return evidenceJson; }
    public Integer getBeforeSchoolDay() { return beforeSchoolDay; }

    public static CommandLog of(Player player, UUID commandId, String kind, String subjectCode, String resultCode,
                                String requestHash, int httpStatus, String responseJson, String afterStatsJson) {
        CommandLog log = new CommandLog();
        log.playerId = player.getId();
        log.commandId = commandId;
        log.year = player.getYear();
        log.turn = player.getTurn();
        log.beforeSchoolDay = player.getBeforeSchoolDay();
        log.kind = kind;
        log.subjectCode = subjectCode;
        log.requestHash = requestHash;
        log.httpStatus = httpStatus;
        log.responseJson = responseJson;
        log.afterStatsJson = afterStatsJson;
        log.resultCode = resultCode;
        return log;
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

    public String getKind() {
        return kind;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseJson() {
        return responseJson;
    }
}
