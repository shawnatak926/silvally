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
}
