package org.example.membership.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.OrderRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RenewalPipelineService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BadgeService badgeService;
    private final MembershipService membershipService;
    private final MembershipLogService membershipLogService;
    private final CouponService couponService;
    private final BadgeRepository badgeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Map<Long, Map<Long, OrderCountAndAmount>> collectStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> aggregates = orderRepository.aggregateByUserAndCategoryBetween(startDateTime, endDateTime);

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = new HashMap<>();

        for (Object[] row : aggregates) {
            Long userId = (Long) row[0];
            Long categoryId = (Long) row[1];
            Long count = ((Number) row[2]).longValue();
            BigDecimal amount = (BigDecimal) row[3];

            Map<Long, OrderCountAndAmount> categoryMap = statMap.computeIfAbsent(userId, k -> new HashMap<>());
            categoryMap.put(categoryId, new OrderCountAndAmount(count, amount));
        }

        return statMap;
    }


    @Transactional
    public void runBadgeOnly(LocalDate targetDate) {
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = collectStats(targetDate);
        List<User> users = userRepository.findAll();

        final int BATCH_SIZE = 1000;
        int count = 0;

        for (User user : users) {
            Map<Long, OrderCountAndAmount> stats = statMap.get(user.getId());
            List<Badge> modifiedBadges = badgeService.updateBadgeStatesForUser(user, stats);

            for (Badge badge : modifiedBadges) {
                badgeRepository.save(badge); // 명시적 업데이트
                count++;

                if (count % BATCH_SIZE == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        }

        entityManager.flush();
        entityManager.clear();
    }


    @Transactional
    public void runLevelOnly() {
        List<User> users = userRepository.findAll();

        Map<Long, Long> activeBadgeMap = badgeRepository.countActiveBadgesGroupedByUserId()
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(), // user_id
                        row -> ((Number) row[1]).longValue()  // count
                ));

        final int BATCH_SIZE = 1000;
        int count = 0;

        for (User user : users) {
            long activeCount = activeBadgeMap.getOrDefault(user.getId(), 0L);
            MembershipLevel prev = user.getMembershipLevel();
            MembershipLevel newLevel = membershipService.calculateLevel(activeCount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            userRepository.save(user);

            count++;
            flushAndClearIfNeeded(count, BATCH_SIZE);
        }

        entityManager.flush();
        entityManager.clear();
    }



    @Transactional
    public void runLogOnly() {
        List<User> users = userRepository.findAll();
        final int BATCH_SIZE = 1000;
        int count = 0;

        for (User user : users) {
            membershipLogService.insertMembershipLog(user, user.getMembershipLevel());
            count++;
            flushAndClearIfNeeded(count, BATCH_SIZE);
        }

        entityManager.flush();
        entityManager.clear();
    }


    @Transactional
    public void runLevelAndLog() {
        List<User> users = userRepository.findAll();

        //  사용자별 활성 배지 수 캐싱
        Map<Long, Long> activeBadgeMap = badgeRepository.countActiveBadgesGroupedByUserId()
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(), // user_id
                        row -> ((Number) row[1]).longValue()  // count
                ));

        //  이전 등급 캐싱: userId → prevLevel
        Map<Long, MembershipLevel> prevLevelMap = new HashMap<>();

        final int BATCH_SIZE = 1000;
        int count = 0;

        long updateTotalTime = 0L;
        long insertTotalTime = 0L;

        // 1차 루프: 등급 갱신 + 이전 등급 캐싱 (→ UPDATE batching 유도)
        for (User user : users) {
            long badgeCount = activeBadgeMap.getOrDefault(user.getId(), 0L);

            long updateStart = System.currentTimeMillis();

            MembershipLevel prevLevel = user.getMembershipLevel();
            MembershipLevel newLevel = membershipService.calculateLevel(badgeCount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());

            prevLevelMap.put(user.getId(), prevLevel);

            updateTotalTime += System.currentTimeMillis() - updateStart;

            count++;
            flushAndClearIfNeeded(count, BATCH_SIZE);
        }

        entityManager.flush();
        entityManager.clear();

        //  2차 루프: 로그 기록 (→ INSERT batching 유도)
        count = 0;

        for (User user : users) {
            long insertStart = System.currentTimeMillis();

            MembershipLevel prev = prevLevelMap.get(user.getId());
            MembershipLevel current = user.getMembershipLevel(); // 이미 변경된 값

            MembershipLog log = new MembershipLog();
            log.setUser(user); // detach 상태여도 ID는 유효
            log.setPreviousLevel(prev);
            log.setNewLevel(current);
            log.setChangeReason("badge count: " + activeBadgeMap.getOrDefault(user.getId(), 0L));

            entityManager.persist(log);

            insertTotalTime += System.currentTimeMillis() - insertStart;

            count++;
            flushAndClearIfNeeded(count, BATCH_SIZE);
        }

        entityManager.flush();
        entityManager.clear();

        System.out.println("등급 업데이트 총 소요 시간: " + updateTotalTime + "ms");
        System.out.println("로그 insert 총 소요 시간: " + insertTotalTime + "ms");
    }


    @Transactional
    public void runCouponOnly() {
        List<User> users = userRepository.findAll();
        couponService.bulkIssueCoupons(users, 1000);
    }




    @Transactional
    public void runFullJpa(LocalDate targetDate) {
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = collectStats(targetDate);
        List<User> users = userRepository.findAll();
        for (User user : users) {
            badgeService.updateBadgeStatesForUser(user, statMap.get(user.getId()));
            var prev = membershipService.updateUserLevel(user);
            membershipLogService.insertMembershipLog(user, prev);
            couponService.issueCoupons(user); // 내부에서 로그 기록까지 수행
        }
    }


    private void flushAndClearIfNeeded(int count, int batchSize) {
        if (count % batchSize == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }

}
