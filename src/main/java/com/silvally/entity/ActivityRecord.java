package com.silvally.entity;

import com.silvally.common.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "activity_record", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "eventCode", "choiceCode"}))
public class ActivityRecord extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private UUID playerId;
    @Column(nullable = false) private String eventCode;
    @Column(nullable = false) private String choiceCode;
    @Column(nullable = false) private String resultCode;
    @Lob private String evidenceJson;
    private Integer beforeSchoolDay;
    protected ActivityRecord() { }
    public ActivityRecord(Player player, String event, String choice, String result, String evidence) {
        playerId = player.getId(); eventCode = event; choiceCode = choice;
        resultCode = result; evidenceJson = evidence; beforeSchoolDay = player.getBeforeSchoolDay();
    }
    public Long getId() { return id; }
    public String getChoiceCode() { return choiceCode; }
    public String getEventCode() { return eventCode; }
    public String getResultCode() { return resultCode; }
    public String getEvidenceJson() { return evidenceJson; }
    public Integer getBeforeSchoolDay() { return beforeSchoolDay; }
}
