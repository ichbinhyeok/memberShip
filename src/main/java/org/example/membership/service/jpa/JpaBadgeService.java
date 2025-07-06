package org.example.membership.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
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
public class JpaBadgeService {

    private final BadgeRepository badgeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
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

    @Transactional
    public void bulkUpdateBadgeStates(List<User> users,
                                      Map<Long, Map<Long, OrderCountAndAmount>> statMap,
                                      int batchSize) {
        int count = 0;

        for (User user : users) {
            Map<Long, OrderCountAndAmount> stats = statMap.get(user.getId());
            List<Badge> modifiedBadges = updateBadgeStatesForUser(user, stats);

            for (Badge badge : modifiedBadges) {
                badgeRepository.save(badge);
                count++;
                flushAndClearIfNeeded(count, batchSize);
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    private void flushAndClearIfNeeded(int count, int batchSize) {
        if (count % batchSize == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }

}
