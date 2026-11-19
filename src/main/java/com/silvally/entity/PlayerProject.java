package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "player_project",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "project_code"}))
public class PlayerProject {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private String projectCode;

    @Column(name = "school_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private int startTurn;

    @Column(nullable = false)
    private int endTurn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Lob
    private String workSnapshotsJson;

    @Column(nullable = false)
    private int docBonus;

    private Integer quality;

    @Column(nullable = false)
    private boolean inPortfolio;

    protected PlayerProject() {
    }

    public static PlayerProject start(UUID playerId, String projectCode, int year, int startTurn, int endTurn) {
        PlayerProject project = new PlayerProject();
        project.playerId = playerId;
        project.projectCode = projectCode;
        project.year = year;
        project.startTurn = startTurn;
        project.endTurn = endTurn;
        project.status = ProjectStatus.IN_PROGRESS;
        project.workSnapshotsJson = "[]";
        return project;
    }

    public void saveSnapshots(String workSnapshotsJson) {
        this.workSnapshotsJson = workSnapshotsJson;
    }

    public void finish(int quality, boolean passed) {
        this.quality = quality;
        this.status = passed ? ProjectStatus.COMPLETE : ProjectStatus.FAILED;
    }

    public void expire() {
        this.status = ProjectStatus.EXPIRED;
    }

    public void registerPortfolio() {
        this.inPortfolio = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public int getStartTurn() {
        return startTurn;
    }

    public int getEndTurn() {
        return endTurn;
    }

    public String getWorkSnapshotsJson() {
        return workSnapshotsJson;
    }

    public int getYear() {
        return year;
    }

    public Integer getQuality() {
        return quality;
    }

    public boolean isInPortfolio() {
        return inPortfolio;
    }
}
