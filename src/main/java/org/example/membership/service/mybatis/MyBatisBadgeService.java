package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyBatisBadgeService {

    private final BadgeMapper badgeMapper;


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
    @Transactional
    public void bulkUpdateBadgeStates(List<User> users,
                                      Map<Long, Map<Long, OrderCountAndAmount>> statMap,
                                      int batchSize) {
        int count = 0;

        for (User user : users) {
            Map<Long, OrderCountAndAmount> stats = statMap.getOrDefault(user.getId(), Collections.emptyMap());
            List<Badge> badges = badgeMapper.findByUserId(user.getId());

            for (Badge badge : badges) {
                OrderCountAndAmount stat = stats.get(badge.getCategory().getId());

                boolean shouldBeActive = stat != null &&
                        stat.getCount() >= 5 &&
                        stat.getAmount().compareTo(new BigDecimal("300000")) >= 0;

                if (badge.isActive() != shouldBeActive) {
                    if (shouldBeActive) {
                        badge.activate();
                    } else {
                        badge.deactivate();
                    }
                    badgeMapper.update(badge); // DB 반영
                    count++;
                }

                if (count % batchSize == 0) {
                    // MyBatis는 flush/clear 없음, 대신 연결 부하 완화용 로그 출력 가능
                    System.out.println("Flushed " + count + " badge updates");
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public long countActiveBadgesByUserId(Long userId) {
        return badgeMapper.countByUserIdAndActiveTrue(userId);
    }


}
