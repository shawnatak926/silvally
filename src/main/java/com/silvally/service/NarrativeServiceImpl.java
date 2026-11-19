package com.silvally.service;

import com.silvally.entity.CommandLog;
import com.silvally.entity.EventChoice;
import com.silvally.entity.EventDef;
import com.silvally.entity.LifeNarrative;
import com.silvally.entity.NarrativeStatus;
import com.silvally.entity.Player;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.EventChoiceRepository;
import com.silvally.repository.EventDefRepository;
import com.silvally.repository.LifeNarrativeRepository;
import com.silvally.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
public class NarrativeServiceImpl implements NarrativeService {

    private static final Logger log = LoggerFactory.getLogger(NarrativeServiceImpl.class);
    private static final int MAX_ERROR_LENGTH = 500;
    private static final int MAX_PREVIOUS_LENGTH = 300;

    private final LifeNarrativeRepository narratives;
    private final CommandLogRepository commandLogs;
    private final PlayerRepository players;
    private final EventDefRepository eventDefs;
    private final EventChoiceRepository eventChoices;
    private final NarrativeGenerator generator;
    private final NarrativeGenerator fallback = new TemplateNarrativeGenerator();
    private final NarrativeContextCollector contextCollector;
    private final ObjectMapper json;
    private final ApplicationEventPublisher publisher;

    public NarrativeServiceImpl(LifeNarrativeRepository narratives, CommandLogRepository commandLogs,
                                PlayerRepository players, EventDefRepository eventDefs,
                                EventChoiceRepository eventChoices, NarrativeGenerator generator,
                                NarrativeContextCollector contextCollector, ObjectMapper json,
                                ApplicationEventPublisher publisher) {
        this.narratives = narratives;
        this.commandLogs = commandLogs;
        this.players = players;
        this.eventDefs = eventDefs;
        this.eventChoices = eventChoices;
        this.generator = generator;
        this.contextCollector = contextCollector;
        this.json = json;
        this.publisher = publisher;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void request(CommandLog sceneLog) {
        LifeNarrative narrative = narratives.findByCommandLogId(sceneLog.getId())
                .orElseGet(() -> narratives.save(LifeNarrative.pending(sceneLog, snapshot(sceneLog))));
        if (narrative.getStatus() != NarrativeStatus.SAVED) {
            publisher.publishEvent(new NarrativeRequestedEvent(narrative.getId()));
        }
    }

    @Override
    public void write(Long narrativeId) {
        Optional<LifeNarrative> found = narratives.findById(narrativeId);
        if (found.isEmpty() || found.get().getStatus() == NarrativeStatus.SAVED) {
            return;
        }
        NarrativeScene scene = null;
        try {
            scene = scene(found.get());
            GeneratedNarrative generated = generator.generate(scene);
            narratives.markSaved(narrativeId, generated.text(), generated.source(), generated.model());
        } catch (RuntimeException e) {
            log.warn("Narrative generation failed: narrativeId={} error={}", narrativeId, e.toString());
            narratives.markFailed(narrativeId, truncate(e.getClass().getSimpleName() + ": " + e.getMessage()),
                    fallbackText(scene));
        }
    }

    private String fallbackText(NarrativeScene scene) {
        if (scene == null) {
            return null;
        }
        try {
            return fallback.generate(scene).text();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private NarrativeScene scene(LifeNarrative narrative) {
        CommandLog sceneLog = commandLogs.findById(narrative.getCommandLogId()).orElseThrow();
        Player player = players.findById(narrative.getPlayerId()).orElseThrow();
        String rulesVersion = player.getRulesVersion();
        Optional<EventDef> def = eventDefs.findById(new EventDef.Id(rulesVersion, narrative.getSceneId()));
        String choiceLabel = eventChoices
                .findById(new EventChoice.Id(rulesVersion, narrative.getSceneId(), narrative.getChoiceCode()))
                .map(EventChoice::getLabel)
                .orElse(narrative.getChoiceCode());
        return new NarrativeScene(narrative.getSceneId(),
                def.map(EventDef::getTitle).orElse(narrative.getSceneId()),
                def.map(EventDef::getSituation).orElse(null),
                narrative.getChoiceCode(), choiceLabel, sceneLog.getResultCode(), sceneLog.getEvidenceJson(),
                sceneLog.getYear(), sceneLog.getTurn(), sceneLog.getBeforeSchoolDay(),
                context(narrative), previousNarratives(narrative));
    }

    private String snapshot(CommandLog sceneLog) {
        Player player = players.findById(sceneLog.getPlayerId()).orElseThrow();
        return json.writeValueAsString(contextCollector.collect(player, sceneLog));
    }

    private NarrativeContext context(LifeNarrative narrative) {
        return narrative.getContextJson() == null ? null
                : json.readValue(narrative.getContextJson(), NarrativeContext.class);
    }

    private List<String> previousNarratives(LifeNarrative narrative) {
        return narratives.findTop3ByPlayerIdAndStatusAndCommandLogIdLessThanOrderByCommandLogIdDesc(
                        narrative.getPlayerId(), NarrativeStatus.SAVED, narrative.getCommandLogId())
                .reversed().stream()
                .map(n -> n.getText().length() <= MAX_PREVIOUS_LENGTH ? n.getText()
                        : n.getText().substring(0, MAX_PREVIOUS_LENGTH))
                .toList();
    }

    private static String truncate(String message) {
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
