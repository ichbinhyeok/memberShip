package org.example.membership.service.jpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.MembershipLevel;

import org.example.membership.dto.MembershipLogRequest;
import org.example.membership.entity.Badge;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.repository.jpa.*;
import org.example.membership.repository.mybatis.MembershipLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaMembershipRenewalService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MembershipLogRepository membershipLogRepository;
    private final MembershipLogMapper membershipLogMapper;
    private final CategoryRepository categoryRepository;
    private final BadgeRepository badgeRepository;
    private final CouponRepository couponRepository;
    private final CouponIssueLogRepository couponIssueLogRepository;

    public void renewMembershipLevel(LocalDate targetDate) {
        StopWatch watch = new StopWatch();
        watch.start(); // 시작 시간 기록

        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3); // 3월 1일
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);     // 5월 31일

        // 1. 유저/카테고리별 주문 통계 조회
        List<Object[]> aggregates = orderRepository.aggregateByUserAndCategoryBetween(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        // 2. Map<Long userId, Map<Long categoryId, Stats>> 형태로 변환
        class Stats { long count; BigDecimal amount; Stats(long c, BigDecimal a){count=c; amount=a;} }
        Map<Long, Map<Long, Stats>> statMap = new java.util.HashMap<>();
        for (Object[] row : aggregates) {
            Long uId = (Long) row[0];
            Long cId = (Long) row[1];
            Long cnt = ((Number) row[2]).longValue();
            BigDecimal amt = (BigDecimal) row[3];
            statMap.computeIfAbsent(uId, k -> new java.util.HashMap<>())
                    .put(cId, new Stats(cnt, amt));
        }

        // 3. 전체 유저 가져오기
        List<User> users = userRepository.findAll();

        // 4. 저장용 리스트 생성
        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLog> logs = new ArrayList<>();
        List<Badge> badgesToUpdate = new ArrayList<>();
        List<Coupon> coupons = new ArrayList<>();
        List<CouponIssueLog> issueLogs = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            Map<Long, Stats> userStats = statMap.getOrDefault(user.getId(), java.util.Collections.emptyMap());

            List<Badge> currentBadges = badgeRepository.findByUser(user);

            for (Badge b : currentBadges) {
                Stats s = userStats.get(b.getCategory().getId());
                if (s != null && s.count >= 5 && s.amount.compareTo(new BigDecimal("300000")) >= 0) {
                    b.activate();
                } else {
                    b.deactivate();
                }
                badgesToUpdate.add(b);
            }

            long badgeCount = currentBadges.stream().filter(Badge::isActive).count();
            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(badgeCount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(now);
            updatedUsers.add(user);

            MembershipLog log = new MembershipLog();
            log.setUser(user);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason("badge count: " + badgeCount);
            log.setChangedAt(now);
            logs.add(log);

            int couponQty = switch (newLevel) {
                case VIP -> 3;
                case GOLD -> 2;
                case SILVER -> 1;
                default -> 0;
            };

            if (couponQty > 0) {
                List<Badge> activeBadges = currentBadges.stream()
                        .filter(Badge::isActive)
                        .toList();
                for (Badge b : activeBadges) {
                    for (int i = 0; i < couponQty; i++) {
                        Coupon c = new Coupon();
                        c.setCode("AUTO-" + java.util.UUID.randomUUID().toString().substring(0,8));
                        c.setDiscountAmount(org.example.membership.common.enums.CouponAmount.W1000);
                        c.setCategory(b.getCategory());
                        coupons.add(c);

                        CouponIssueLog logEntry = new CouponIssueLog();
                        logEntry.setUser(user);
                        logEntry.setCoupon(c);
                        logEntry.setMembershipLevel(newLevel);
                        logEntry.setIssuedAt(now);
                        issueLogs.add(logEntry);
                    }
                }
            }
        }

        badgeRepository.saveAll(badgesToUpdate);
        userRepository.saveAll(updatedUsers);
        membershipLogRepository.saveAll(logs);
        couponRepository.saveAll(coupons);
        couponIssueLogRepository.saveAll(issueLogs);

        watch.stop(); // 종료 시간 기록
        log.info("✅ 등급 갱신 배치 완료 - 처리 유저 수: {}, 총 소요 시간: {} ms",
                updatedUsers.size(),
                watch.getTotalTimeMillis());
    }
    public void renewMembershipLevelJpaUpdateInsertForeach(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<Object[]> aggregates = orderRepository.aggregateByUserAndCategoryBetween(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        class Stats { long count; BigDecimal amount; Stats(long c, BigDecimal a){count=c; amount=a;} }
        Map<Long, Map<Long, Stats>> statMap = new java.util.HashMap<>();
        for (Object[] row : aggregates) {
            Long uId = (Long) row[0];
            Long cId = (Long) row[1];
            Long cnt = ((Number) row[2]).longValue();
            BigDecimal amt = (BigDecimal) row[3];
            statMap.computeIfAbsent(uId, k -> new java.util.HashMap<>())
                    .put(cId, new Stats(cnt, amt));
        }
        List<User> users = userRepository.findAll();

        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLogRequest> logs = new ArrayList<>();
        List<Badge> badgesToUpdate = new ArrayList<>();
        List<Coupon> coupons = new ArrayList<>();
        List<CouponIssueLog> issueLogs = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            Map<Long, Stats> userStats = statMap.getOrDefault(user.getId(), java.util.Collections.emptyMap());
            List<Badge> currentBadges = badgeRepository.findByUser(user);
            for (Badge b : currentBadges) {
                Stats s = userStats.get(b.getCategory().getId());
                if (s != null && s.count >= 5 && s.amount.compareTo(new BigDecimal("300000")) >= 0) {
                    b.activate();
                } else {
                    b.deactivate();
                }
                badgesToUpdate.add(b);
            }

            long badgeCount = currentBadges.stream().filter(Badge::isActive).count();
            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(badgeCount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(now);
            updatedUsers.add(user);

            MembershipLogRequest logReq = new MembershipLogRequest();
            logReq.setUserId(user.getId());
            logReq.setPreviousLevel(oldLevel);
            logReq.setNewLevel(newLevel);
            logReq.setChangeReason("badge count: " + badgeCount);
            logReq.setChangedAt(now);
            logs.add(logReq);

            int couponQty = switch (newLevel) {
                case VIP -> 3;
                case GOLD -> 2;
                case SILVER -> 1;
                default -> 0;
            };
            if (couponQty > 0) {
                List<Badge> activeBadges = currentBadges.stream()
                        .filter(Badge::isActive)
                        .toList();
                for (Badge b : activeBadges) {
                    for (int i = 0; i < couponQty; i++) {
                        Coupon c = new Coupon();
                        c.setCode("AUTO-" + java.util.UUID.randomUUID().toString().substring(0,8));
                        c.setDiscountAmount(org.example.membership.common.enums.CouponAmount.W1000);
                        c.setCategory(b.getCategory());
                        coupons.add(c);

                        CouponIssueLog entry = new CouponIssueLog();
                        entry.setUser(user);
                        entry.setCoupon(c);
                        entry.setMembershipLevel(newLevel);
                        entry.setIssuedAt(now);
                        issueLogs.add(entry);
                    }
                }
            }
        }
        badgeRepository.saveAll(badgesToUpdate);
        userRepository.saveAll(updatedUsers);
        membershipLogMapper.bulkInsertRequests(logs);
        couponRepository.saveAll(coupons);
        couponIssueLogRepository.saveAll(issueLogs);
    }

    private MembershipLevel calculateLevel(long badgeCount) {
        if (badgeCount >= 3) {
            return MembershipLevel.VIP;
        } else if (badgeCount == 2) {
            return MembershipLevel.GOLD;
        } else if (badgeCount == 1) {
            return MembershipLevel.SILVER;
        }
        return MembershipLevel.NONE;
    }


}