// org/example/membership/batch/BatchScheduler.java
package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.WasInstance;
import org.example.membership.infra.cluster.ScaleOutNotifier;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchScheduler {

    private final SnapshotBatchOrchestrator orchestrator;
    private final FlagManager flagManager;
    private final ScaleOutNotifier scaleOutNotifier;
    private final WasInstanceRepository wasInstanceRepository;
    private final MyWasInstanceHolder myWasInstanceHolder;

    @Scheduled(cron = "0 0 0 1 * *") // 매월 1일 00:00
    public void runBadgeBatch() {
        // 1) 모든 WAS: 로컬 배지 플래그 ON
        flagManager.addBadgeFlag(-1L, -1L);

        LocalDate targetDate = LocalDate.now();
        int batchSize = 500;

        boolean leaderExecuted = false;
        try {
            // 2) 오케스트레이터 실행(선점 실패 시 false)
            leaderExecuted = orchestrator.runFullBatch(targetDate, batchSize);
        } catch (Exception e) {
            log.error("[배지 배치] 스케줄러 예외", e);
            // 선점 성공 후 실패했을 수 있으므로 플래그 해제 브로드캐스트를 시도해야 합니다.
            leaderExecuted = true;
        } finally {
            if (leaderExecuted) {
                try {
                    // 3) 살아있는 '다른' 인스턴스 조회(자기 자신 제외)
                    LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
                    List<WasInstance> others = wasInstanceRepository.findAliveInstances(threshold).stream()
                            .sorted(Comparator.comparing(WasInstance::getRegisteredAt))
                            .filter(w -> !w.getId().equals(myWasInstanceHolder.getMyUuid()))
                            .toList();

                    // 4) 배지 플래그 해제 알림
                    scaleOutNotifier.notifyBadgeFlagOffToOthers(others);
                } catch (Exception e) {
                    log.error("[배지 배치] 플래그 해제 실패", e);
                } finally {
                    // 5) 내 로컬도 OFF
                    flagManager.removeBadgeFlag(-1L, -1L);
                }
            }
        }
    }
}
