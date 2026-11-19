package com.silvally.entity;

import com.silvally.common.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "field_learning")
public class FieldLearning extends BaseEntity {
    public enum Path { DOMESTIC, SILICON_VALLEY }
    public enum Status { SELECTED, NOT_SELECTED, PREPARING, COMPLETED }
    @Id private UUID playerId;
    @Version private Long version;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Path path;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(nullable = false) private int englishAtApplication;
    private String failureReason;
    private int appliedYear;
    private int appliedTurn;
    private Integer preparedTurn;
    private Integer completedTurn;
    public int getAppliedYear() { return appliedYear; }
    public int getAppliedTurn() { return appliedTurn; }
    public Integer getPreparedTurn() { return preparedTurn; }
    public Integer getCompletedTurn() { return completedTurn; }
    protected FieldLearning() { }
    public FieldLearning(Player p, Path path) {
        appliedYear = p.getYear(); appliedTurn = p.getTurn();
        playerId = p.getId(); this.path = path; englishAtApplication = p.getStats().getEn();
        status = path == Path.DOMESTIC || englishAtApplication >= 50 ? Status.SELECTED : Status.NOT_SELECTED;
        failureReason = status == Status.NOT_SELECTED ? "ENGLISH_BELOW_50" : null;
    }
    public Path getPath() { return path; }
    public Status getStatus() { return status; }
    public int getEnglishAtApplication() { return englishAtApplication; }
    public String getFailureReason() { return failureReason; }
    public void prepare() { if (status == Status.SELECTED) { status = Status.PREPARING; preparedTurn = 3; } }
    public void complete() { if (status == Status.PREPARING) { status = Status.COMPLETED; completedTurn = 4; } }
}
