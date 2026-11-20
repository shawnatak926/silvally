package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * 한 회차의 한 학년 나르샤 프로젝트. 회차+학년마다 한 건이라 2학년 시작이 1학년 기록을 덮어쓰지 않는다.
 * 주제는 서버가 검증한 문자열로만 저장하며 점수·보상 계산에는 쓰이지 않는다.
 */
@Entity
@Table(name = "narsha_project",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "school_year"}))
public class NarshaProject {
    public static final String SOURCE_CANDIDATE = "CANDIDATE";
    public static final String SOURCE_CUSTOM = "CUSTOM";
    public static final String SOURCE_LEGACY = "LEGACY";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "school_year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NarshaStatus status;

    @Column(nullable = false, length = 24)
    private String topicSource;

    @Column(length = 40)
    private String topicCode;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(nullable = false, length = 24)
    private String role;

    @Column(nullable = false)
    private int startTurn;

    private Integer endTurn;

    @Column(nullable = false)
    private int workCount;

    @Column(nullable = false)
    private int mentoringCount;

    /** 마지막으로 작업·멘토링 행동을 한 학년 내 턴. 같은 턴에 한 번만 하게 한다. */
    private Integer lastActionTurn;

    private Integer quality;

    @Column(name = "result_rank")
    private Integer rank;

    private String rankSource;

    @Column(nullable = false)
    private boolean exhibited;

    protected NarshaProject() {
    }

    public static NarshaProject begin(UUID playerId, int year, int turn, String topicSource, String topicCode,
                                      String topic, String role) {
        NarshaProject project = new NarshaProject();
        project.playerId = playerId;
        project.year = year;
        project.status = NarshaStatus.IN_PROGRESS;
        project.topicSource = topicSource;
        project.topicCode = topicCode;
        project.topic = topic;
        project.role = role;
        project.startTurn = turn;
        return project;
    }

    public void addWork(int turn) {
        workCount++;
        lastActionTurn = turn;
    }

    public void addMentoring(int turn) {
        mentoringCount++;
        lastActionTurn = turn;
    }

    /** 이벤트 선택으로 쌓이는 기록은 행동 턴 제한과 별개라서 횟수만 올린다. */
    public void countWorkFromEvent() {
        workCount++;
    }

    public void countMentoringFromEvent() {
        mentoringCount++;
    }

    public void wrap(Integer quality, int turn) {
        this.quality = quality;
        this.endTurn = turn;
        this.status = NarshaStatus.WRAPPED;
    }

    public void rank(Integer quality, int rank, String rankSource, int turn) {
        this.quality = quality;
        this.rank = rank;
        this.rankSource = rankSource;
        this.endTurn = turn;
        this.status = NarshaStatus.RANKED;
    }

    public void exhibit() {
        this.exhibited = true;
    }

    public UUID getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public int getYear() { return year; }
    public NarshaStatus getStatus() { return status; }
    public String getTopicSource() { return topicSource; }
    public String getTopicCode() { return topicCode; }
    public String getTopic() { return topic; }
    public String getRole() { return role; }
    public int getStartTurn() { return startTurn; }
    public Integer getEndTurn() { return endTurn; }
    public int getWorkCount() { return workCount; }
    public int getMentoringCount() { return mentoringCount; }
    public Integer getLastActionTurn() { return lastActionTurn; }
    public Integer getQuality() { return quality; }
    public Integer getRank() { return rank; }
    public String getRankSource() { return rankSource; }
    public boolean isExhibited() { return exhibited; }
}
