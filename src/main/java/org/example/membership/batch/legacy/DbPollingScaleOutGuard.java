//package org.example.membership.batch;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.membership.repository.jpa.WasInstanceRepository;
//import org.example.membership.exception.ScaleOutInterruptedException;
//import org.springframework.beans.factory.DisposableBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import jakarta.annotation.PostConstruct;
//import java.time.LocalDateTime;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class DbPollingScaleOutGuard implements ScaleOutGuard, DisposableBean {
//
//    private final WasInstanceRepository wasInstanceRepository;
//
//    @Value("${batch.heartbeat-timeout-seconds:30}")
//    private int heartbeatTimeoutSeconds;
//
//    @Value("${batch.scale-guard.poll-interval-millis:2000}")
//    private long pollIntervalMillis;
//
//    private final ScheduledExecutorService scheduler =
//            Executors.newSingleThreadScheduledExecutor(r -> {
//                Thread t = new Thread(r, "scale-guard-poller");
//                t.setDaemon(true);
//                return t;
//            });
//
//    /** 마지막 폴링 결과(메모리 캐시). 초기엔 -1. */
//    private volatile long lastAlive = -1;
//
//    /** 배치 시작 시점의 기대 WAS 수. */
//    private volatile long expected = -1;
//
//    @PostConstruct
//    void start() {
//        scheduler.scheduleAtFixedRate(this::poll, 0, pollIntervalMillis, TimeUnit.MILLISECONDS);
//    }
//
//    private void poll() {
//        try {
//            LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
//            long alive = wasInstanceRepository.countAliveInstances(threshold);
//            if (alive != lastAlive) {
//                log.debug("[ScaleGuard] alive 변경: {} -> {}", lastAlive, alive);
//                lastAlive = alive;
//            }
//        } catch (Exception e) {
//            // 폴링 실패는 치명 아님. 다음 주기에 재시도
//            log.warn("[ScaleGuard] 폴링 실패: {}", e.getMessage());
//        }
//    }
//
//    /** 필요 시 즉시 1회 동기 폴링 */
//    private void pollOnce() {
//        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
//        long alive = wasInstanceRepository.countAliveInstances(threshold);
//        lastAlive = alive;
//    }
//
//    @Override
//    public void init(long expectedTotal) {
//        this.expected = expectedTotal;
//        pollOnce(); // 초기 블라인드 제거
//        log.info("[ScaleGuard] init: expected={}, currentAlive={}", expected, lastAlive);
//    }
//
//    @Override
//    public void ensureUnchanged() {
//        long alive = lastAlive;
//        long exp = expected;
//        if (exp >= 0 && alive >= 0 && alive != exp) {
//            throw new ScaleOutInterruptedException(
//                    "WAS 수 변경 감지: 기존=" + exp + ", 현재=" + alive
//            );
//        }
//    }
//
//    @Override
//    public long currentAlive() {
//        return lastAlive;
//    }
//
//    @Override
//    public void destroy() {
//        scheduler.shutdownNow();
//    }
//}
