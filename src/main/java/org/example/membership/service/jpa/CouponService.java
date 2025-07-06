package org.example.membership.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.CouponAmount;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.CouponIssueLogRepository;
import org.example.membership.repository.jpa.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final BadgeRepository badgeRepository;
    private final CouponRepository couponRepository;
    private final CouponIssueLogRepository couponIssueLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<CouponIssueLog> issueCoupons(User user) {
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
    public void bulkIssueCoupons(List<User> users, int batchSize) {
        Map<Long, List<Badge>> badgeMap = badgeRepository.findAllByUserInAndActiveTrue(users).stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        Map<Long, Coupon> couponMap = couponRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getCategory().getId(), c -> c));

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

                for (int i = 0; i < qty; i++) {
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


}
