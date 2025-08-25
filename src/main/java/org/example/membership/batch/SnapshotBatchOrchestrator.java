package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.batch.BatchExecutionLog;
import org.example.membership.repository.jpa.batch.BatchExecutionLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotBatchOrchestrator {
    private final BatchExecutionLogTx logTx;
    private final ReadPhaseService readPhaseService;
    private final WritePhaseService writePhaseService;

    public boolean runFullBatch(LocalDate targetDate, int batchSize) {
        UUID exec = UUID.randomUUID();
        LocalDateTime t0 = LocalDateTime.now();
        LocalDateTime cutoff = targetDate.atStartOfDay();

        if (!logTx.lockStart(exec, targetDate.toString(), cutoff)) {
            log.warn("다른 배치 실행 중, 건너뜀");
            return false;
        }

        try {
            /* CalcContext {
                    List<User> myUsers,
                    Map<String, Boolean> keysToUpdate, = <userId + ":" + categoryId,배지 활성화여부>
                    int batchSize,
                    boolean empty,
                    LocalDateTime batchStartTime
            }

            * */
            CalcContext ctx = readPhaseService.buildContext(targetDate, cutoff, batchSize, t0);
            if (ctx.empty()) {
                logTx.markCompleted(exec);
                return true;
            }

            // Phase 1~4 순서대로 명시 호출
            // ctx에 있는 keyToUpdate를 통해 어떤 유저의 어떤 카테고리의 배지를 활성화할지 Result 스냅샷 테이블에 저장
            writePhaseService.produceBadgeResults(exec, ctx);
            writePhaseService.applyBadges(exec, t0);

            writePhaseService.produceLevelResults(exec, ctx);
            writePhaseService.applyLevels(exec, t0);

            // 쿠폰 적용 단계는 그대로 마지막
            writePhaseService.applyCoupon(exec, ctx);

            logTx.markCompleted(exec);
            return true;
        } catch (Exception e) {
            logTx.markFailed(exec);
            throw e;
        }
    }
}
