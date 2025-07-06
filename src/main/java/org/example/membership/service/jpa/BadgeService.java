package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Category;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;

    public List<Badge> updateBadgeStatesForUser(User user, Map<Long, OrderCountAndAmount> statsByCategory) {
        if (statsByCategory == null) {
            statsByCategory = Collections.emptyMap();
        }

        List<Badge> modifiedBadges = new java.util.ArrayList<>();
        List<Badge> badges = badgeRepository.findByUser(user);

        for (Badge badge : badges) {
            OrderCountAndAmount stat = statsByCategory.get(badge.getCategory().getId());

            boolean shouldBeActive = stat != null &&
                    stat.getCount() >= 3 &&
                    stat.getAmount().compareTo(new BigDecimal("100000")) >= 0;

            if (badge.isActive() != shouldBeActive) {
                if (shouldBeActive) {
                    badge.activate();
                } else {
                    badge.deactivate();
                }
                modifiedBadges.add(badge);
            }
        }

        return modifiedBadges;
    }
}
