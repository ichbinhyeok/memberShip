package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.BatchExecutionLog.BatchStatus;
import org.example.membership.repository.jpa.BatchExecutionLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchRestoreTrigger {

    private final BatchExecutionLogRepository batchRepo;
    private final FlagAwareBatchOrchestrator orchestrator;
    private final FlagManager flagManager;

    // 필요 시 cron/주기 조정
    @Scheduled(fixedDelay = 3000L)
    @Transactional
    public void tick() {
        String targetDate = LocalDate.now().toString();

        long active = batchRepo.countActiveForTargetDate(targetDate, BatchStatus.RUNNING  , BatchStatus.RESTORING);
        if (active > 0) {
            return; // 아직 실행/복원 중인 배치가 있음 → 복원 금지
        }

        List<BatchExecutionLog> candidates = batchRepo.findInterruptedScaleOut(targetDate, BatchStatus.INTERRUPTED);
        if (candidates.isEmpty()) {
            return;
        }

        if (!flagManager.isGlobalApiGateOn()) {
            flagManager.turnOnGlobalApiGate();
            log.info("[게이트 ON] 복원 시작");
        }

        for (BatchExecutionLog b : candidates) {
            int ok = batchRepo.tryAcquireRestore(b.getId());
            if (ok == 1) {
                log.info("[복원 선점] batchId={} targetDate={}", b.getId(), targetDate);
                try {
                    // 여기서는 간단히 전체 오케스트레이터를 실행합니다.
                    // 복원 전용 경로가 있다면 그걸 호출하세요.
                    orchestrator.runFullBatch(targetDate, /*batchSize*/ 1000);
                    b.markCompleted();
                    batchRepo.save(b);
                } catch (Exception ex) {
                    log.error("[복원 실패] batchId={}", b.getId(), ex);
                    b.markInterruptedByScaleOut();
                    batchRepo.save(b);
                }
                break; // 한 건만 복원하고 종료(정책에 따라 모두 돌려도 됨)
            }
        }
    }
}
