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

    /** 지원과 서류 평가 결과를 함께 기록한다. 서류에서 떨어지면 바로 REJECTED. */
    public static JobApplication apply(UUID playerId, String companyCode, String role, int appliedTurn,
                                       int portfolioScore, int documentScore, boolean passed) {
        JobApplication application = new JobApplication();
        application.playerId = playerId;
        application.companyCode = companyCode;
        application.role = role;
        application.appliedTurn = appliedTurn;
        application.p = BigDecimal.valueOf(portfolioScore);
        application.d = BigDecimal.valueOf(documentScore);
        application.status = passed ? ApplicationStatus.DOC_PASSED : ApplicationStatus.REJECTED;
        application.failureCode = passed ? null : "DOC_FAILED";
        return application;
    }

    public void recordCoding(int score, boolean passed) {
        k = BigDecimal.valueOf(score);
        status = passed ? ApplicationStatus.CODE_PASSED : ApplicationStatus.REJECTED;
        failureCode = passed ? null : "CODING_FAILED";
    }

    /** 회사 면접 점수와 통과 여부를 기록한다. */
    public void recordInterview(int score, boolean passed) {
        j = BigDecimal.valueOf(score);
        status = passed ? ApplicationStatus.INTERVIEW_PASSED : ApplicationStatus.REJECTED;
        failureCode = passed ? null : "INTERVIEW_FAILED";
    }

    public void recordResult(int composite, boolean offered) {
        q = BigDecimal.valueOf(composite);
        status = offered ? ApplicationStatus.OFFERED : ApplicationStatus.REJECTED;
        failureCode = offered ? null : "RESULT_FAILED";
    }

    public void expire() {
        status = ApplicationStatus.EXPIRED;
        failureCode = "DEADLINE_PASSED";
    }

    public int getAppliedTurn() {
        return appliedTurn;
    }

    public BigDecimal getQ() {
        return q;
    }

    public BigDecimal getD() {
        return d;
    }

    public BigDecimal getK() {
        return k;
    }

    public BigDecimal getJ() {
        return j;
    }

    public BigDecimal getP() {
        return p;
    }

    public String getFailureCode() {
        return failureCode;
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
