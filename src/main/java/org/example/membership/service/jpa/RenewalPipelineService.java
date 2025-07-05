package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.BadgeService.Stats;
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

@Service
@RequiredArgsConstructor
public class RenewalPipelineService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BadgeService badgeService;
    private final MembershipService membershipService;
    private final MembershipLogService membershipLogService;
    private final CouponService couponService;
    private final CouponLogService couponLogService;


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
        for (User user : users) {
            badgeService.updateBadgeStatesForUser(user, statMap.get(user.getId()));
        }
    }

    @Transactional
    public void runLevelOnly() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            membershipService.updateUserLevel(user);
        }
    }

    @Transactional
    public void runLogOnly() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            membershipLogService.insertMembershipLog(user, user.getMembershipLevel());
        }
    }

    @Transactional
    public void runLevelAndLog() {
        List<User> users = userRepository.findAll();

        long updateTotalTime = 0L;
        long insertTotalTime = 0L;

        for (User user : users) {
            MembershipLevel previous = user.getMembershipLevel();

            long updateStart = System.currentTimeMillis();
            MembershipLevel newLevel = membershipService.updateUserLevel(user); // 변경 수행
            updateTotalTime += System.currentTimeMillis() - updateStart;

            long insertStart = System.currentTimeMillis();
            membershipLogService.insertMembershipLog(user, previous); // 변경 전 등급으로 로그 기록
            insertTotalTime += System.currentTimeMillis() - insertStart;
        }

        System.out.println("등급 업데이트 총 소요 시간: " + updateTotalTime + "ms");
        System.out.println("로그 insert 총 소요 시간: " + insertTotalTime + "ms");
    }


    @Transactional
    public void runCouponOnly() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            couponService.issueCoupons(user);
        }
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

}
