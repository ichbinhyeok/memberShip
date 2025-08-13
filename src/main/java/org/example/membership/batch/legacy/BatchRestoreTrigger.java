//package org.example.membership.batch;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.membership.entity.batch.BatchExecutionLog;
//import org.example.membership.entity.batch.BatchExecutionLog.BatchStatus;
//import org.example.membership.repository.jpa.batch.BatchExecutionLogRepository;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class BatchRestoreTrigger {
//
//    private static final int HEARTBEAT_TIMEOUT_SECONDS = 30;
//
//
//    private final BatchExecutionLogRepository batchRepo;
//    // 의존성 변경: FlagAware... -> Snapshot...
//    private final SnapshotBatchOrchestrator orchestrator;
//    // FlagManager 의존성 제거
//
//    @Scheduled(fixedDelay = 30000L) // 체크 주기를 30초로 늘림
//    @Transactional
//    public void tick() {
//        // 오늘 날짜뿐만 아니라, 최근에 중단된 모든 배치를 대상으로 할 수 있음 (정책에 따라)
//        // 여기서는 간단히 오늘 날짜만 확인
//        String targetDate = LocalDate.now().toString();
//
//        // threshold는 하트비트 타임아웃 기준
//        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
//
//// 현재 실행 중이거나 복원 중인 배치 중, WAS가 살아있는 것만 카운트
//        long active = batchRepo.countAliveActiveForTargetDate(
//                targetDate,
//                BatchStatus.RUNNING,
//                BatchStatus.RESTORING,
//                threshold
//        );
//
//        if (active > 0) {
//            return;
//        }
//
//
//        // 중단된 배치 목록을 찾음
//        List<BatchExecutionLog> candidates = batchRepo.findByStatus(BatchStatus.INTERRUPTED);
//        if (candidates.isEmpty()) {
//            return;
//        }
//
//        // 전역 게이트 제어 로직 완전 삭제
//
//        log.info("[복원 감지] 중단된 배치 {}건 발견", candidates.size());
//
//        // 한 번에 하나의 배치만 복원 시도
//        for (BatchExecutionLog interruptedBatch : candidates) {
//            // 다른 WAS가 먼저 복원을 시작하지 않았는지 확인 (선점)
//            int ok = batchRepo.tryAcquireRestore(interruptedBatch.getId());
//            if (ok == 1) {
//                log.info("[복원 선점] batchId={}, executionId={}", interruptedBatch.getId(), interruptedBatch.getExecutionId());
//                try {
//                    // 복원 전용 메서드 호출
//                    orchestrator.restoreInterruptedBatch(interruptedBatch);
//                    // 복원이 성공적으로 끝나면 Orchestrator 내부에서 상태를 변경하므로, 여기서는 별도 처리 안 함
//                } catch (Exception ex) {
//                    log.error("[복원 실패] batchId={}", interruptedBatch.getId(), ex);
//                    // 실패 시 다시 INTERRUPTED 상태로 돌리거나, FAILED 상태로 변경 (정책에 따라)
//                    interruptedBatch.markFailed(); // 예: 복원 실패 시 FAILED로 처리
//                    batchRepo.save(interruptedBatch);
//                }
//                break; // 한 번의 tick에서는 하나의 배치만 처리
//            }
//        }
//    }
//}
