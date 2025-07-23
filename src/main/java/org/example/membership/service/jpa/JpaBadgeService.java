package org.example.membership.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Category;
import org.example.membership.entity.User;
import org.example.membership.exception.NotFoundException;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.CategoryRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaBadgeService {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final JpaOrderService jpaOrderService;
    private final FlagManager flagManager;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * [1] 특정 유저에 대한 배지 상태 일괄 갱신 (API 단위)
     */
    @Transactional
    public List<Badge> updateBadgeStatesForUser(User user, Map<Long, OrderCountAndAmount> statsByCategory) {
        if (statsByCategory == null) statsByCategory = Collections.emptyMap();

        List<Badge> modifiedBadges = new ArrayList<>();
        List<Badge> badges = badgeRepository.findByUser(user);

        for (Badge badge : badges) {
            OrderCountAndAmount stat = statsByCategory.get(badge.getCategory().getId());

            boolean shouldBeActive = stat != null &&
                    stat.getCount() >= 3 &&
                    stat.getAmount().compareTo(new BigDecimal("100000")) >= 0;

            if (badge.isActive() != shouldBeActive) {
                if (shouldBeActive) badge.activate();
                else badge.deactivate();
                modifiedBadges.add(badge);
            }
        }

        return modifiedBadges;
    }

    @Transactional
    public void bulkUpdateBadgeStates(List<String> keysToUpdate, int batchSize) {
        if (keysToUpdate == null || keysToUpdate.isEmpty()) return;

        // 1. 미리 모든 배지를 조회하여 맵으로 구성 (N+1 문제 최소화)
        Set<Long> userIds = new HashSet<>();
        for (String key : keysToUpdate) {
            String[] parts = key.split(":");
            userIds.add(Long.parseLong(parts[0]));
        }

        List<Badge> badges = badgeRepository.findAllByUserIdIn(userIds);
        Map<String, Badge> badgeMap = new HashMap<>();
        for (Badge badge : badges) {
            String mapKey = badge.getUser().getId() + ":" + badge.getCategory().getId();
            badgeMap.put(mapKey, badge);
        }

        List<Badge> toUpdate = new ArrayList<>();
        for (String key : keysToUpdate) {
            Badge badge = badgeMap.get(key);
            if (badge == null) continue;

            if (badge.isActive()) badge.deactivate();
            else badge.activate();

            toUpdate.add(badge);
        }

        for (int i = 0; i < toUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toUpdate.size());
            List<Badge> chunk = toUpdate.subList(i, end);
            badgeRepository.saveAll(chunk);
            entityManager.flush();
            entityManager.clear();
        }
    }

    /**
     * [2] 전체 유저에 대한 배치 단위 배지 상태 병렬 갱신
     */
    @Transactional
    public void bulkUpdateBadgeStates(List<User> users,
                                      Map<Long, Map<Long, OrderCountAndAmount>> statMap,
                                      int batchSize) {

        List<Badge> allBadges = badgeRepository.findAllByUserIn(users);
        Map<Long, List<Badge>> badgeMap = allBadges.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        List<Badge> toUpdate = new ArrayList<>();

        for (User user : users) {
            Map<Long, OrderCountAndAmount> stats = statMap.getOrDefault(user.getId(), Collections.emptyMap());
            List<Badge> badges = badgeMap.getOrDefault(user.getId(), Collections.emptyList());

            for (Badge badge : badges) {
                Long userId = badge.getUser().getId();
                Long categoryId = badge.getCategory().getId();

                // 실시간과의 충돌 방지를 위한 row-level 락
                if (!flagManager.addBadgeFlag(userId, categoryId)) continue;

                try {
                    OrderCountAndAmount stat = stats.get(categoryId);

                    boolean shouldBeActive = stat != null &&
                            stat.getCount() >= 5 &&
                            stat.getAmount().compareTo(new BigDecimal("400000")) >= 0;




                    if (badge.isActive() != shouldBeActive) {
                        if (shouldBeActive) badge.activate();
                        else badge.deactivate();
                        toUpdate.add(badge);
                    }
                } finally {
                    flagManager.removeBadgeFlag(userId, categoryId); // 처리 완료 후 해제
                }
            }
        }

        // flush 단위로 저장 수행
        for (int i = 0; i < toUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toUpdate.size());
            List<Badge> chunk = toUpdate.subList(i, end);
            badgeRepository.saveAll(chunk);
            entityManager.flush();
            entityManager.clear();
        }
    }

    /**
     * [3] 실시간 수동 배지 수정 API (플래그 기반 차단 포함)
     */
    @Transactional
    public Badge changeBadgeActivation(Long userId, Long categoryId, boolean active) {
        if (flagManager.isBadgeBatchRunning() || flagManager.isBadgeFlagged(userId, categoryId)) {
            throw new IllegalStateException("현재 해당 배지는 배치 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Badge badge = badgeRepository.findByUserAndCategory(user, category)
                .orElseThrow(() -> new NotFoundException("Badge not found"));

        if (active) badge.activate();
        else badge.deactivate();

        return badgeRepository.save(badge);
    }

    /**
     * [4] 단일 배지 업데이트 (오늘 기준 통계 기반)
     */
    @Transactional
    public Badge updateBadge(Long userId, Long categoryId) {
        if (flagManager.isBadgeBatchRunning() || flagManager.isBadgeFlagged(userId, categoryId)) {
            throw new IllegalStateException("현재 해당 배지는 배치 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Badge badge = badgeRepository.findByUserAndCategory(user, category)
                .orElseThrow(() -> new NotFoundException("Badge not found"));

        Map<Long, Map<Long, OrderCountAndAmount>> statMap =
                jpaOrderService.aggregateUserCategoryStats(LocalDate.now());
        Map<Long, OrderCountAndAmount> userStats = statMap.getOrDefault(userId, Collections.emptyMap());
        OrderCountAndAmount stat = userStats.get(categoryId);

        boolean shouldBeActive = stat != null &&
                stat.getCount() >= 5 &&
                stat.getAmount().compareTo(new BigDecimal("400000")) >= 0;

        if (badge.isActive() != shouldBeActive) {
            if (shouldBeActive) badge.activate();
            else badge.deactivate();
        }

        return badgeRepository.save(badge);
    }

    /**
     * 현재 배지 상태와 기대 상태를 비교하여, 상태 변경이 필요한 (userId:categoryId) 키를 반환
     */
    public List<String> detectBadgeUpdateTargets(List<User> users,
                                                 Map<Long, Map<Long, OrderCountAndAmount>> statMap) {
        List<Badge> allBadges = badgeRepository.findAllByUserIn(users);
        List<String> targets = new ArrayList<>();

        for (Badge badge : allBadges) {
            Long userId = badge.getUser().getId();
            Long categoryId = badge.getCategory().getId();
            OrderCountAndAmount stat = statMap.getOrDefault(userId, Collections.emptyMap()).get(categoryId);

            boolean shouldBeActive = stat != null &&
                    stat.getCount() >= 5 &&
                    stat.getAmount().compareTo(new BigDecimal("400000")) >= 0;

            if (badge.isActive() != shouldBeActive) {
                targets.add(userId + ":" + categoryId);
            }
        }

        return targets;
    }

}
