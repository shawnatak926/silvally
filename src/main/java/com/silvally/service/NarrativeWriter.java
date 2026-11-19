package com.silvally.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NarrativeWriter {

    private static final Logger log = LoggerFactory.getLogger(NarrativeWriter.class);

    private final NarrativeService narrativeService;
    private final TaskExecutor executor;

    public NarrativeWriter(NarrativeService narrativeService, @Qualifier("narrativeExecutor") TaskExecutor executor) {
        this.narrativeService = narrativeService;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(NarrativeRequestedEvent event) {
        try {
            executor.execute(() -> narrativeService.write(event.narrativeId()));
        } catch (TaskRejectedException e) {
            // 대기열이 가득 차면 PENDING으로 남겨 두고 재시도 API에서 다시 생성한다.
            log.warn("Narrative generation queue full: narrativeId={}", event.narrativeId());
        }
    }
}
