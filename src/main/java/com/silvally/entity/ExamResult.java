package com.silvally.entity;

import com.silvally.common.BaseEntity;
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

/** 학기별 중간·수행·기말 평가 결과. 같은 학기의 같은 평가는 한 번만 저장된다. */
@Entity
@Table(name = "exam_result",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "school_year", "semester", "kind"}))
public class ExamResult extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "school_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private int semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamKind kind;

    @Column(name = "school_turn", nullable = false)
    private int turn;

    @Column(nullable = false)
    private int preparation;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int gradeBefore;

    @Column(nullable = false)
    private int gradeAfter;

    protected ExamResult() {
    }

    public ExamResult(Player player, int semester, ExamKind kind, int preparation, int score,
                      int gradeBefore, int gradeAfter) {
        this.playerId = player.getId();
        this.year = player.getYear();
        this.turn = player.getTurn();
        this.semester = semester;
        this.kind = kind;
        this.preparation = preparation;
        this.score = score;
        this.gradeBefore = gradeBefore;
        this.gradeAfter = gradeAfter;
    }

    public Long getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public int getYear() { return year; }
    public int getSemester() { return semester; }
    public ExamKind getKind() { return kind; }
    public int getTurn() { return turn; }
    public int getPreparation() { return preparation; }
    public int getScore() { return score; }
    public int getGradeBefore() { return gradeBefore; }
    public int getGradeAfter() { return gradeAfter; }
}
