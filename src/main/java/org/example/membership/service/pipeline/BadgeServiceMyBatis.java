package org.example.membership.service.pipeline;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.UserCategoryStats;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BadgeServiceMyBatis {

    private final BadgeMapper badgeMapper;

    @Transactional
    public void updateBadgeStatesForUser(User user, Map<Long, UserCategoryStats> statsByCategory) {
        if (statsByCategory == null) {
            statsByCategory = Collections.emptyMap();
        }
        List<Badge> badges = badgeMapper.findByUserId(user.getId());
        for (Badge badge : badges) {
            UserCategoryStats stat = statsByCategory.get(badge.getCategory().getId());
            if (stat != null && stat.getCount() >= 5 && stat.getAmount().compareTo(new BigDecimal("300000")) >= 0) {
                badge.setActive(true);
            } else {
                badge.setActive(false);
            }
            badge.setUpdatedAt(LocalDateTime.now());
            badgeMapper.update(badge);
        }
    }
}
