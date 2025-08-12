package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaBadgeService {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final JpaOrderService jpaOrderService;

    // private final FlagManager flagManager; // 더 이상 배치 플래그에 의존하지 않음

    /**
     * [배치용] 현재 배지 상태와 통계를 비교하여, 상태 변경이 필요한 (userId:categoryId) 키와
     * 새로운 상태(true/false)를 Map 형태로 반환합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Boolean> detectBadgeUpdateTargets(List<User> users,
                                                         Map<Long, Map<Long, OrderCountAndAmount>> statMap) {
        List<Badge> allBadges = badgeRepository.findAllByUserIn(users);
        Map<String, Boolean> targets = new HashMap<>();

        for (Badge badge : allBadges) {
            Long userId = badge.getUser().getId();
            Long categoryId = badge.getCategory().getId();
            OrderCountAndAmount stat = statMap.getOrDefault(userId, new HashMap<>()).get(categoryId);

            // 배지 획득/유지 조건
            boolean shouldBeActive = stat != null &&
                    stat.getCount() >= 5 &&
                    stat.getAmount().compareTo(new BigDecimal("400000")) >= 0;

            if (badge.isActive() != shouldBeActive) {
                //  key-value 형태로 새로운 상태(shouldBeActive)를 함께 저장
                targets.put(userId + ":" + categoryId, shouldBeActive);
            }
        }
        return targets;
    }

    /**
     * [API용] 단일 배지 상태를 '오늘 기준 통계'로 즉시 업데이트합니다.
     */
    @Transactional
    public Badge updateBadge(Long userId, Long categoryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        Badge badge = badgeRepository.findByUserAndCategory(user, category)
                .orElseThrow(() -> new NotFoundException("Badge not found"));

        // ✅ 에러 수정: 두 번째 인자로 현재 시간을 전달합니다.
        Map<Long, Map<Long, OrderCountAndAmount>> statMap =
                jpaOrderService.aggregateUserCategoryStats(LocalDate.now(), LocalDateTime.now());

        Map<Long, OrderCountAndAmount> userStats = statMap.getOrDefault(userId, Collections.emptyMap());
        OrderCountAndAmount stat = userStats.get(categoryId);

        boolean shouldBeActive = stat != null &&
                stat.getCount() >= 5 &&
                stat.getAmount().compareTo(new BigDecimal("400000")) >= 0;

        if (badge.isActive() != shouldBeActive) {
            if (shouldBeActive) {
                badge.activate();
            } else {
                badge.deactivate();
            }
        }
        return badgeRepository.save(badge);
    }

    /**
     * [API용] 관리자가 배지 활성 상태를 수동으로 변경합니다.
     */
    @Transactional
    public Badge changeBadgeActivation(Long userId, Long categoryId, boolean active) {
        // ✅ 단순화: 더 이상 배치 실행 여부를 확인할 필요가 없습니다.
        // if (flagManager.isBadgeBatchRunning() || flagManager.isBadgeFlagged(userId, categoryId)) {
        //     throw new IllegalStateException("현재 해당 배지는 배치 처리 중입니다. 잠시 후 다시 시도해주세요.");
        // }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        Badge badge = badgeRepository.findByUserAndCategory(user, category)
                .orElseThrow(() -> new NotFoundException("Badge not found"));

        if (active) {
            badge.activate();
        } else {
            badge.deactivate();
        }

        return badgeRepository.save(badge);
    }
}
