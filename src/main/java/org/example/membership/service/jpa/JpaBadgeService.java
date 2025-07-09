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
import java.util.stream.Collectors;

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

        // Retrieve all badges for the given users in a single query
        List<Badge> allBadges = badgeRepository.findAllByUserIn(users);

        // Group badges by user id for quick lookup
        Map<Long, List<Badge>> badgeMap = allBadges.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

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

                    badgeRepository.save(badge); //명시적으로 알 수 있게
                    count++;
                    flushAndClearIfNeeded(count, batchSize);
                }
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
