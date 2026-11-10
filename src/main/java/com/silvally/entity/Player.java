package com.silvally.entity;

import com.silvally.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "player")
public class Player extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 24)
    private String name;

    @Column(nullable = false)
    private String rulesVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Phase phase;

    @Column(name = "school_year", nullable = false)
    private int year;

    @Column(name = "school_turn", nullable = false)
    private int turn;

    @Column(nullable = false)
    private int ap;

    @Embedded
    private Stats stats;

    private String major;

    private String track;

    @Column(nullable = false)
    private String applicationProfile;

    private String admissionTrack;

    private Integer beforeSchoolDay;

    private String initialCareerCode;

    private String currentCareerCode;

    @Lob
    private String recommendationJson;

    @Column(nullable = false)
    private int kitCount;

    @Column(nullable = false)
    private int resumeLevel;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private String seed;

    private String endingCode;

    @Lob
    private String endingJson;

    @Column(unique = true)
    private String createKey;

    private String createRequestHash;

    @Lob
    private String createResponseJson;

    private UUID lastCommandId;

    protected Player() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        return version;
    }
}
