package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.dto.UserCategoryOrderStats;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.repository.mybatis.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class MyBatisBadgeService {

    private final BadgeMapper badgeMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;


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
    public void bulkUpdateBadgeStates(LocalDate targetDate) {
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = collectStats(targetDate);
        List<User> users = userMapper.findAll();
        for (User user : users) {
            updateBadgeStatesForUser(user, statMap.get(user.getId()));
        }
    }

    private Map<Long, Map<Long, OrderCountAndAmount>> collectStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<UserCategoryOrderStats> aggregates =
                orderMapper.aggregateByUserAndCategoryBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = new HashMap<>();

        for (UserCategoryOrderStats row : aggregates) {
            Long userId = row.getUserId();
            Long categoryId = row.getCategoryId();
            long count = row.getOrderCount();
            BigDecimal amount = row.getTotalAmount();

            statMap.computeIfAbsent(userId, k -> new HashMap<>())
                    .put(categoryId, new OrderCountAndAmount(count, amount));
        }

        return statMap;
    }
}
