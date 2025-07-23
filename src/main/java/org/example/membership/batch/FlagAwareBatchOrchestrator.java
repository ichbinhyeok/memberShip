package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FlagAwareBatchOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FlagAwareBatchOrchestrator.class);

    private final JpaOrderService jpaOrderService;
    private final JpaBadgeService jpaBadgeService;
    private final JpaMembershipService jpaMembershipService;
    private final FlagManager flagManager;

    private final BadgeBatchExecutor badgeBatchExecutor;
    private final UserLevelBatchExecutor userLevelBatchExecutor;
    private final CouponBatchExecutor couponBatchExecutor;

    public void runFullBatch(String targetDate, int batchSize) {
        if (flagManager.isBadgeBatchRunning()) {
            log.warn("[전체 배치 중복 실행 차단] 이미 전역 배지 배치가 진행 중입니다.");
            return;
        }

        flagManager.startGlobalBadgeBatch();
        List<User> users;
        Map<Long, Map<Long, OrderCountAndAmount>> statMap;
        List<String> keysToFlag;
        Instant allStart = Instant.now();
        long time1; long time2; long time3;
        try {
            LocalDate date = LocalDate.parse(targetDate);

            Instant t1 = Instant.now();
            statMap = jpaOrderService.aggregateUserCategoryStats(date);
            time1 = Duration.between(t1, Instant.now()).toMillis();

            Instant t2 = Instant.now();
            users = jpaMembershipService.getAllUsers();
            time2 = Duration.between(t2, Instant.now()).toMillis();

            Instant t3 = Instant.now();
            keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);
            time3 = Duration.between(t3, Instant.now()).toMillis();
        } catch (Exception e) {
            flagManager.endGlobalBadgeBatch();
            throw new RuntimeException(e);
        }
        flagManager.endGlobalBadgeBatch();

        Instant t4 = Instant.now();
        badgeBatchExecutor.execute(keysToFlag, batchSize);
        long time4 = Duration.between(t4, Instant.now()).toMillis();

        Instant t5 = Instant.now();
        userLevelBatchExecutor.execute(users, batchSize);
        long time5 = Duration.between(t5, Instant.now()).toMillis();

        Instant t6 = Instant.now();
        couponBatchExecutor.execute(users, batchSize);
        long time6 = Duration.between(t6, Instant.now()).toMillis();

        long total = Duration.between(allStart, Instant.now()).toMillis();
        log.info("[전체 배치 완료] 총 소요 시간: {}ms", total);
        log.info(" ├─ [1] 주문 통계 집계: {}ms", time1);
        log.info(" ├─ [2] 유저 조회: {}ms", time2);
        log.info(" ├─ [3] 배지 갱신 대상 추출 및 플래그 설정: {}ms", time3);
        log.info(" ├─ [4] 배지 갱신: {}ms", time4);
        log.info(" ├─ [5] 등급 갱신: {}ms", time5);
        log.info(" └─ [6] 쿠폰 발급: {}ms", time6);
    }

    public void runBadge(String targetDate, int batchSize) {
        if (flagManager.isBadgeBatchRunning()) {
            log.warn("[배지 배치 중복 실행 차단] 현재 전역 배치가 진행 중입니다.");
            return;
        }

        flagManager.startGlobalBadgeBatch();
        try {
            List<User> users = jpaMembershipService.getAllUsers();
            Map<Long, Map<Long, OrderCountAndAmount>> statMap = jpaOrderService.aggregateUserCategoryStats(LocalDate.parse(targetDate));
            List<String> keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);
            badgeBatchExecutor.execute(keysToFlag, batchSize);
        } finally {
            flagManager.endGlobalBadgeBatch();
        }
    }

    public void runUserLevel(int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        userLevelBatchExecutor.execute(users, batchSize);
    }

    public void runCoupon(int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        couponBatchExecutor.execute(users, batchSize);
    }
}
