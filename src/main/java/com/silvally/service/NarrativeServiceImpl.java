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

import java.util.Optional;

@Service
public class NarrativeServiceImpl implements NarrativeService {

    private static final Logger log = LoggerFactory.getLogger(NarrativeServiceImpl.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final LifeNarrativeRepository narratives;
    private final CommandLogRepository commandLogs;
    private final PlayerRepository players;
    private final EventDefRepository eventDefs;
    private final EventChoiceRepository eventChoices;
    private final NarrativeGenerator generator;
    private final ApplicationEventPublisher publisher;

    public NarrativeServiceImpl(LifeNarrativeRepository narratives, CommandLogRepository commandLogs,
                                PlayerRepository players, EventDefRepository eventDefs,
                                EventChoiceRepository eventChoices, NarrativeGenerator generator,
                                ApplicationEventPublisher publisher) {
        this.narratives = narratives;
        this.commandLogs = commandLogs;
        this.players = players;
        this.eventDefs = eventDefs;
        this.eventChoices = eventChoices;
        this.generator = generator;
        this.publisher = publisher;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void request(CommandLog sceneLog) {
        LifeNarrative narrative = narratives.findByCommandLogId(sceneLog.getId())
                .orElseGet(() -> narratives.save(LifeNarrative.pending(sceneLog)));
        if (narrative.getStatus() != NarrativeStatus.SAVED) {
            publisher.publishEvent(new NarrativeRequestedEvent(narrative.getId()));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Long narrativeId) {
        Optional<LifeNarrative> found = narratives.findById(narrativeId);
        if (found.isEmpty() || found.get().getStatus() == NarrativeStatus.SAVED) {
            return;
        }
        try {
            String text = generator.generate(scene(found.get()));
            narratives.markSaved(narrativeId, text);
        } catch (RuntimeException e) {
            log.warn("Narrative generation failed: narrativeId={}", narrativeId, e);
            narratives.markFailed(narrativeId, truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
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
                sceneLog.getYear(), sceneLog.getTurn(), sceneLog.getBeforeSchoolDay());
    }

    private static String truncate(String message) {
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
