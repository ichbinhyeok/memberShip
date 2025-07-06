package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Category;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.OrderRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
    public void bulkUpdateBadgeStates(LocalDate targetDate) {
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = collectStats(targetDate);
        List<User> users = userRepository.findAll();

        final int BATCH_SIZE = 1000;
        int count = 0;

        for (User user : users) {
            Map<Long, OrderCountAndAmount> stats = statMap.get(user.getId());
            List<Badge> modifiedBadges = updateBadgeStatesForUser(user, stats);

            for (Badge badge : modifiedBadges) {
                badgeRepository.save(badge);
                count++;
                flushAndClearIfNeeded(count, BATCH_SIZE);
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    private Map<Long, Map<Long, OrderCountAndAmount>> collectStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> aggregates = orderRepository.aggregateByUserAndCategoryBetween(startDateTime, endDateTime);

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = new HashMap<>();

        for (Object[] row : aggregates) {
            Long userId = (Long) row[0];
            Long categoryId = (Long) row[1];
            Long count = ((Number) row[2]).longValue();
            BigDecimal amount = (BigDecimal) row[3];

            Map<Long, OrderCountAndAmount> categoryMap = statMap.computeIfAbsent(userId, k -> new HashMap<>());
            categoryMap.put(categoryId, new OrderCountAndAmount(count, amount));
        }

        return statMap;
    }

    private void flushAndClearIfNeeded(int count, int batchSize) {
        if (count % batchSize == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}
