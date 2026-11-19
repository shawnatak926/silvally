package com.silvally.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NarrativeWriter {

    private final NarrativeService narrativeService;

    public NarrativeWriter(NarrativeService narrativeService) {
        this.narrativeService = narrativeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(NarrativeRequestedEvent event) {
        narrativeService.write(event.narrativeId());
    }
}
