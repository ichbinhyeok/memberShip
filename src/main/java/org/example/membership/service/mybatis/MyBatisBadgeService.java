package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.dto.UserBadgeCount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyBatisBadgeService {

    private final BadgeMapper badgeMapper;

    private final SqlSessionFactory sqlSessionFactory;


    @Transactional
    public void updateBadgeStatesForUser(User user, Map<Long, OrderCountAndAmount> statsByCategory) {
        if (statsByCategory == null) {
            statsByCategory = Collections.emptyMap();
        }
        List<Badge> badges = badgeMapper.findByUserId(user.getId());
        for (Badge badge : badges) {
            OrderCountAndAmount stat = statsByCategory.get(badge.getCategory().getId());
            if (stat != null && stat.getCount() >= 3 && stat.getAmount().compareTo(new BigDecimal("100000")) >= 0) {
                badge.activate();
            } else {
                badge.deactivate();
            }
            badgeMapper.update(badge);
        }
    }

//    @Transactional Execute.BATCH일때는 수동으로 커밋을 하므로 @Transactional ㄴ
public void bulkUpdateBadgeStates(List<User> users,
                                  Map<Long, Map<Long, OrderCountAndAmount>> statMap,
                                  int batchSize) {

    Instant start = Instant.now();

    List<Long> userIds = users.stream()
            .map(User::getId)
            .collect(Collectors.toList());

    try (SqlSession session = sqlSessionFactory.openSession(false)) {
        BadgeMapper badgeMapper = session.getMapper(BadgeMapper.class);

        // ✅ 전체 유저 배지 조회
        List<Badge> allBadges = badgeMapper.findByUserIds(userIds);

        // ✅ userId → List<Badge>로 그룹핑
        Map<Long, List<Badge>> badgeMap = allBadges.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        // ✅ 변경 대상만 수집
        List<Badge> toUpdate = new ArrayList<>();

        for (User user : users) {
            Map<Long, OrderCountAndAmount> stats = statMap.getOrDefault(user.getId(), Collections.emptyMap());
            List<Badge> badges = badgeMap.getOrDefault(user.getId(), Collections.emptyList());

            for (Badge badge : badges) {
                OrderCountAndAmount stat = stats.get(badge.getCategory().getId());

                boolean shouldBeActive = stat != null &&
                        stat.getCount() >= 5 &&
                        stat.getAmount().compareTo(new BigDecimal("400000")) >= 0;

                if (badge.isActive() != shouldBeActive) {
                    if (shouldBeActive) {
                        badge.activate();
                    } else {
                        badge.deactivate();
                    }

                    toUpdate.add(badge);
                }
            }
        }

        // ✅ 순수 Java로 리스트를 chunk 처리
        int total = 0;
        for (int i = 0; i < toUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toUpdate.size());
            List<Badge> chunk = toUpdate.subList(i, end);
            badgeMapper.bulkUpdateBadgesCaseWhen(chunk);
            total += chunk.size();
        }

        session.commit();

        log.info("[MyBatis CASE WHEN] 배지 갱신 완료: {}건 처리, {}ms 소요",
                total, Duration.between(start, Instant.now()).toMillis());

    } catch (Exception e) {
        log.error("배지 갱신 중 오류 발생", e);
        throw new RuntimeException("배지 갱신 실패", e);
    }
}



    @Transactional(readOnly = true)
    public long countActiveBadgesByUserId(Long userId) {
        return badgeMapper.countByUserIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countActiveBadgesForUsers(List<Long> userIds) {
        List<UserBadgeCount> list = badgeMapper.countActiveBadgesForUsers(userIds);

        Map<Long, Long> result = new HashMap<>();
        for (UserBadgeCount item : list) {
            result.put(item.getUserId(), item.getBadgeCount());
        }

        return result;
    }



}
