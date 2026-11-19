package com.silvally.entity;

import com.silvally.common.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "interview_question", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "kind", "subject_code"}))
public class InterviewQuestion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID playerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InterviewKind kind;
    @Column(nullable = false) private String subjectCode;
    @Column(nullable = false, length = 1200) private String question;
    @Column(nullable = false) private String source;
    private boolean completed;
    @Column(length = 1200) private String answer;
    public boolean isCompleted() { return completed; }
    public void complete(String answer) { this.answer = answer; completed = true; }
    protected InterviewQuestion() { }
    public InterviewQuestion(UUID playerId, InterviewKind kind, String subjectCode, String question, String source) {
        this.playerId = playerId; this.kind = kind; this.subjectCode = subjectCode;
        this.question = question; this.source = source;
    }
    public UUID getId() { return id; }
    public String getQuestion() { return question; }
    public String getSource() { return source; }
    public InterviewKind getKind() { return kind; }
}
