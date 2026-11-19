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
@Table(name = "job_application",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "company_code"}))
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private String companyCode;

    @Column(nullable = false)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private int appliedTurn;

    private BigDecimal q;

    private BigDecimal d;

    private BigDecimal k;

    private BigDecimal j;

    private BigDecimal p;

    private Integer r;

    private String failureCode;

    protected JobApplication() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }

    /** 졸업 시점에 수락할 수 있는 오퍼(수락 대기 또는 이미 수락) 여부. */
    public boolean isOffer() {
        return status == ApplicationStatus.OFFERED || status == ApplicationStatus.ACCEPTED;
    }

    public void accept() {
        if (!isOffer()) {
            throw new IllegalStateException("오퍼 상태가 아니라 수락할 수 없습니다: " + status);
        }
        status = ApplicationStatus.ACCEPTED;
    }

    public void decline() {
        if (!isOffer()) {
            throw new IllegalStateException("오퍼 상태가 아니라 거절할 수 없습니다: " + status);
        }
        status = ApplicationStatus.DECLINED;
    }
}
