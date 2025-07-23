package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.common.util.ShardPrefixedUuidGenerator;
import org.example.membership.dto.CouponIssueLogDto;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.example.membership.repository.mybatis.CouponIssueLogMapper;
import org.example.membership.repository.mybatis.CouponMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.f4b6a3.uuid.UuidCreator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyBatisCouponService {

    private final CouponIssueLogMapper couponIssueLogMapper;
    private final BadgeMapper badgeMapper;
    private final CouponMapper couponMapper;
    private final SqlSessionFactory sqlSessionFactory;

    @Transactional
    public void insertCouponLogs(User user, List<Coupon> coupons) {
        int shardNo = Math.abs(user.getId().hashCode() % 100); // 한번만 계산

        for (Coupon coupon : coupons) {
            CouponIssueLogDto dto = new CouponIssueLogDto();
            dto.setId(ShardPrefixedUuidGenerator.generate(shardNo)); // String ID
            dto.setUserId(user.getId());
            dto.setCouponId(coupon.getId());
            dto.setMembershipLevel(user.getMembershipLevel());
            dto.setIssuedAt(LocalDateTime.now());

            couponIssueLogMapper.insert(dto);
        }
    }

    public void bulkIssueCoupons(List<User> users,
                                 Map<Long, List<Badge>> badgeMap,
                                 Map<Long, Coupon> couponMap,
                                 Map<String, Long> issuedCountMap,
                                 int batchSize) {

        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            CouponIssueLogMapper mapper = session.getMapper(CouponIssueLogMapper.class);
            List<CouponIssueLogDto> buffer = new ArrayList<>();
            int totalCount = 0;

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
                        CouponIssueLogDto dto = new CouponIssueLogDto();
                        int shardNo = Math.abs(user.getId().hashCode() % 100); // 샤드 번호 결정
                        dto.setId(ShardPrefixedUuidGenerator.generate(shardNo)); // String 기반 ID
                        dto.setUserId(user.getId());
                        dto.setCouponId(coupon.getId());
                        dto.setMembershipLevel(user.getMembershipLevel());
                        dto.setIssuedAt(LocalDateTime.now());

                        buffer.add(dto);
                        totalCount++;

                        if (buffer.size() >= batchSize) {
                            mapper.insertAll(buffer);
                            session.flushStatements();
                            buffer.clear();
                            System.out.println("Flushed " + totalCount + " coupon logs");
                        }
                    }
                }
            }

            if (!buffer.isEmpty()) {
                mapper.insertAll(buffer);
                session.flushStatements();
                System.out.println("Final flush of " + buffer.size() + " coupon logs");
            }

            session.commit();
        }
    }

    @Transactional
    public void bulkIssueCouponsWithResolvedData(List<User> users, int batchSize) {
        List<Long> userIds = users.stream().map(User::getId).toList();

        List<Badge> allBadges = badgeMapper.findByUserIds(userIds);
        Map<Long, List<Badge>> badgeMap = allBadges.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        Map<Long, Coupon> couponMap = couponMapper.findAll().stream()
                .collect(Collectors.toMap(c -> c.getCategory().getId(), c -> c));

        Map<String, Long> issuedCountMap = couponIssueLogMapper.countIssuedPerUserAndCoupon();

        bulkIssueCoupons(users, badgeMap, couponMap, issuedCountMap, batchSize);
    }

    @Transactional
    public void issueCouponsForUsers(List<User> users, int batchSize) {
        // Optional alias for external caller
        bulkIssueCouponsWithResolvedData(users, batchSize);
    }
}
