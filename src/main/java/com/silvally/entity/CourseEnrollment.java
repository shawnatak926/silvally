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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "course_enrollment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "course_code", "enrolled_year"}))
public class CourseEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private int enrolledYear;

    private Integer attendTurn1;

    private Integer attendTurn2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    private BigDecimal examScore;

    private Integer earnedAtTurn;

    protected CourseEnrollment() {
    }

    public static CourseEnrollment enroll(UUID playerId, String courseCode, int year) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.playerId = playerId;
        enrollment.courseCode = courseCode;
        enrollment.enrolledYear = year;
        enrollment.status = CourseStatus.ENROLLED;
        return enrollment;
    }

    public int attendanceCount() {
        return (attendTurn1 == null ? 0 : 1) + (attendTurn2 == null ? 0 : 1);
    }

    public boolean attendedAt(int turn) {
        return Integer.valueOf(turn).equals(attendTurn1) || Integer.valueOf(turn).equals(attendTurn2);
    }

    public void attend(int turn) {
        if (attendTurn1 == null) {
            attendTurn1 = turn;
        } else {
            attendTurn2 = turn;
        }
    }

    public void recordExam(BigDecimal score, boolean passed, int turn) {
        examScore = score;
        status = passed ? CourseStatus.PASSED : CourseStatus.FAILED;
        earnedAtTurn = passed ? turn : null;
    }

    public void expire() {
        status = CourseStatus.EXPIRED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public int getEnrolledYear() {
        return enrolledYear;
    }

    public BigDecimal getExamScore() {
        return examScore;
    }

    public Integer getEarnedAtTurn() {
        return earnedAtTurn;
    }
}
