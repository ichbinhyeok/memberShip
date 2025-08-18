// org/example/membership/infra/admin/AdminBatchController.java
package org.example.membership.infra.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.batch.SnapshotBatchOrchestrator;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.WasInstance;
import org.example.membership.infra.cluster.ScaleOutNotifier;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/internal/batch")
@RequiredArgsConstructor
@Slf4j
public class AdminBatchController {

    private final SnapshotBatchOrchestrator orchestrator;
    private final FlagManager flagManager;
    private final ScaleOutNotifier notifier;
    private final WasInstanceRepository wasRepo;
    private final MyWasInstanceHolder myWas;

    // 예: POST /internal/batch/run?date=2025-09-01&size=500
    @PostMapping("/run")
    public ResponseEntity<String> runOnce(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "500") int size
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        boolean leaderExecuted = false;

        // 1) 모든 WAS에 배지 플래그 ON 전파 + 내 로컬 ON
        flagManager.addBadgeFlag(-1L, -1L);
        try {
            List<WasInstance> others = findOthers();
            notifier.notifyBadgeFlagOnToOthers(others);

            // 2) 배치 실행(선점 실패 시 false)
            leaderExecuted = orchestrator.runFullBatch(targetDate, size);

            return ResponseEntity.ok("runFullBatch=" + leaderExecuted + " date=" + targetDate + " size=" + size);
        } catch (Exception e) {
            log.error("[수동 배치] 실행 예외", e);
            // 선점 성공 후 실패했을 수 있으므로 OFF는 시도
            leaderExecuted = true;
            return ResponseEntity.internalServerError().body("error: " + e.getMessage());
        } finally {
            if (leaderExecuted) {
                try {
                    List<WasInstance> others = findOthers();
                    notifier.notifyBadgeFlagOffToOthers(others);
                } catch (Exception e) {
                    log.error("[수동 배치] 플래그 해제 브로드캐스트 실패", e);
                } finally {
                    flagManager.removeBadgeFlag(-1L, -1L);
                }
            }
        }
    }

    private List<WasInstance> findOthers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        return wasRepo.findAliveInstances(threshold).stream()
                .sorted(Comparator.comparing(WasInstance::getRegisteredAt))
                .filter(w -> !w.getId().equals(myWas.getMyUuid()))
                .toList();
    }
}
