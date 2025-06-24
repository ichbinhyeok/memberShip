package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
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

    public record Stats(long count, BigDecimal amount) {}

    @Transactional
    public void updateBadgeStatesForUser(User user, Map<Long, Stats> statsByCategory) {
        if (statsByCategory == null) {
            statsByCategory = Collections.emptyMap();
        }
        List<Badge> badges = badgeRepository.findByUser(user);
        for (Badge badge : badges) {
            Stats stat = statsByCategory.get(badge.getCategory().getId());
            if (stat != null && stat.count >= 5 && stat.amount.compareTo(new BigDecimal("300000")) >= 0) {
                badge.activate();
            } else {
                badge.deactivate();
            }
        }
    }
}
