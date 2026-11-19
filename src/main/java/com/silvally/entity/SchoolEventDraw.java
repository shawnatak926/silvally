package com.silvally.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** A draw, including no-event outcomes, is immutable once offered. */
@Entity
@Table(name = "school_event_draw", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "period_key", "draw_kind"}))
public class SchoolEventDraw {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "player_id", nullable = false) private UUID playerId;
    @Column(name = "period_key", nullable = false) private String periodKey;
    @Column(name = "draw_kind", nullable = false) private String drawKind;
    @Lob @Column(nullable = false) private String eventJson;
    private boolean resolved;
    protected SchoolEventDraw() { }
    public SchoolEventDraw(UUID playerId, String period, String kind, String eventJson) {
        this.playerId = playerId; periodKey = period; drawKind = kind; this.eventJson = eventJson;
    }
    public String getEventJson() { return eventJson; }
    public boolean isResolved() { return resolved; }
    public void resolve() { resolved = true; }
}
