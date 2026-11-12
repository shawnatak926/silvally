package com.silvally.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name = "event_command", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "requestKey"}))
public class EventCommand {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private UUID playerId;
    @Column(nullable = false, length = 200) private String requestKey;
    @Lob private String requestJson;
    @Lob private String responseJson;
    protected EventCommand() { }
    public EventCommand(UUID player, String key, String request, String response) {
        playerId = player; requestKey = key; requestJson = request; responseJson = response;
    }
    public String getRequestJson() { return requestJson; }
    public String getResponseJson() { return responseJson; }
}
