package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.batch.BatchExecutionLog;
import org.example.membership.repository.jpa.batch.BatchExecutionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

//  배치 실행 로그 전용 트랜잭션 빈
@Service
@RequiredArgsConstructor
public class BatchExecutionLogTx {
    private final BatchExecutionLogRepository repo;

    @Transactional // REQUIRED (기본): Orchestrator가 무트랜잭션이면 새로 열림
    public boolean lockStart(UUID execId, String key, LocalDateTime cutoffAt) {
        return repo.insertIfNotRunning(execId, key, cutoffAt) > 0;
    }

    @Transactional
    public void markCompleted(UUID execId) {
        repo.findByExecutionId(execId).ifPresent(BatchExecutionLog::markCompleted);
        // 변경감지로 update flush
    }

    @Transactional
    public void markFailed(UUID execId) {
        repo.findByExecutionId(execId).ifPresent(BatchExecutionLog::markFailed);
    }
}

