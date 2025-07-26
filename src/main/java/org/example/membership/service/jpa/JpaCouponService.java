package org.example.membership.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.exception.NotFoundException;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.CouponIssueLogRepository;
import org.example.membership.repository.jpa.CouponRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JpaCouponService {

    private final BadgeRepository badgeRepository;
    private final CouponRepository couponRepository;
    private final CouponIssueLogRepository couponIssueLogRepository;
    private final UserRepository userRepository;

    private final MyWasInstanceHolder myWasInstanceHolder;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<CouponIssueLog> issueCoupons(User user) {
        // [WAS Sharding Logic]
        if (!myWasInstanceHolder.isMyUser(user.getId())) {
            return new ArrayList<>();
        }

        int qty = switch (user.getMembershipLevel()) {
            case VIP -> 3;
            case GOLD -> 2;
            case SILVER -> 1;
            default -> 0;
        };

        List<CouponIssueLog> issuedLogs = new ArrayList<>();
        if (qty == 0) return issuedLogs;

        List<Badge> badges = badgeRepository.findByUserAndActiveTrue(user);

        for (Badge badge : badges) {
            Coupon coupon = couponRepository
                    .findByCategory(badge.getCategory())
                    .orElse(null);  // autoIssue 조건 제거
            if (coupon == null) continue;

            int already = couponIssueLogRepository.countByUserAndCoupon(user, coupon);

            for (int i = already; i < qty; i++) {
                CouponIssueLog log = new CouponIssueLog();
                log.setUser(user);
                log.setCoupon(coupon);
                log.setMembershipLevel(user.getMembershipLevel());
                log.setIssuedAt(LocalDateTime.now());
                couponIssueLogRepository.save(log);
                issuedLogs.add(log);
            }
        }

        return issuedLogs;
    }

    @Transactional
    public CouponIssueLog manualIssueCoupon(Long userId, String couponCode) {
        // [WAS Sharding Logic]
        if (!myWasInstanceHolder.isMyUser(userId)) {
            return null;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new NotFoundException("Coupon not found"));

        CouponIssueLog log = new CouponIssueLog();
        log.setUser(user);
        log.setCoupon(coupon);
        log.setMembershipLevel(user.getMembershipLevel());
        log.setIssuedAt(LocalDateTime.now());

        return couponIssueLogRepository.save(log);
    }


    @Transactional
    public void bulkIssueCoupons(List<User> users, int batchSize) {

        // [WAS Sharding Logic]
        users = users.stream()
                .filter(u -> myWasInstanceHolder.isMyUser(u.getId()))
                .toList();

        // 1. 사용자별 배지 사전 조회
        Map<Long, List<Badge>> badgeMap = badgeRepository.findAllByUserInAndActiveTrue(users).stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        // 2. 전체 쿠폰 사전 조회 (카테고리 기준)
        Map<Long, Coupon> couponMap = couponRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getCategory().getId(), c -> c));

        // 3. 기존 발급 로그를 미리 조회하여 캐싱 (userId + couponId 기준)
        List<CouponIssueLog> existingLogs = couponIssueLogRepository.findAllByUserIn(users);
        Map<String, Long> issuedCountMap = existingLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getUser().getId() + "-" + log.getCoupon().getId(),
                        Collectors.counting()
                ));

        int count = 0;

        for (User user : users) {
            int qty = switch (user.getMembershipLevel()) {
                case VIP -> 3;
                case GOLD -> 2;
                case SILVER -> 1;
                default -> 0;
            };
            if (qty == 0) continue;

            List<Badge> badges = badgeMap.getOrDefault(user.getId(), List.of());

            for (Badge badge : badges) {
                Coupon coupon = couponMap.get(badge.getCategory().getId());
                if (coupon == null) continue;

                String key = user.getId() + "-" + coupon.getId();
                long already = issuedCountMap.getOrDefault(key, 0L);

                for (int i = (int) already; i < qty; i++) {
                    CouponIssueLog log = new CouponIssueLog();
                    log.setUser(user);
                    log.setCoupon(coupon);
                    log.setMembershipLevel(user.getMembershipLevel());
                    log.setIssuedAt(LocalDateTime.now());

                    entityManager.persist(log);

                    count++;
                    if (count % batchSize == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                }
            }
        }

        entityManager.flush();
        entityManager.clear();
    }


    public List<CouponIssueLog> getIssuedCouponsByUser(Long userId) {

        // [WAS Sharding Logic]
        if (!myWasInstanceHolder.isMyUser(userId)) {
            return List.of();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return couponIssueLogRepository.findByUser(user);

    }
}
