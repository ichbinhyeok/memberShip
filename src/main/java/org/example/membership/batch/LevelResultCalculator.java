package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.example.membership.entity.batch.LevelResult;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// LevelResultCalculator.java
@Component
@RequiredArgsConstructor
@Slf4j
public class LevelResultCalculator {

    private final LevelResultRepository levelResultRepository;
    private final BadgeRepository badgeRepository;

    public void calculateAndStoreResults(UUID executionId, List<User> users, int batchSize /*, LocalDateTime t0 무시 */) {
        if (users == null || users.isEmpty()) {
            log.warn("[등급 계산 스킵] 대상 없음. executionId={}", executionId);
            return;
        }

        log.info("[등급 결과 계산 시작] 대상: {}건, executionId={}", users.size(), executionId);

        Map<Long, Long> badgeCnt = getActiveBadgeCountMapCurrent(users);

        List<LevelResult> results = new ArrayList<>();
        for (User u : users) {
            long cnt = badgeCnt.getOrDefault(u.getId(), 0L);
            MembershipLevel newLevel = determineNewLevel(cnt);
            if (u.getMembershipLevel() != newLevel) {
                results.add(LevelResult.builder()
                        .executionId(executionId)
                        .userId(u.getId())
                        .newLevel(newLevel)
                        .build());
            }
        }

        if (!results.isEmpty()) {
            // 필요 시 batchSize로 나눠 saveAll
            levelResultRepository.saveAll(results);
            log.info("[등급 결과 저장 완료] {}건", results.size());
        } else {
            log.info("[등급 결과 계산 완료] 변경 없음");
        }
    }

    private Map<Long, Long> getActiveBadgeCountMapCurrent(List<User> users) {
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Object[]> rows = badgeRepository.countActiveBadges(userIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] r : rows) {
            Long userId = ((Number) r[0]).longValue();
            Long cnt = ((Number) r[1]).longValue();
            map.put(userId, cnt);
        }
        return map;
    }

    private MembershipLevel determineNewLevel(long activeBadgeCount) {
        if (activeBadgeCount >= 6) return MembershipLevel.VIP;
        if (activeBadgeCount >= 4) return MembershipLevel.GOLD;
        if (activeBadgeCount >= 2) return MembershipLevel.SILVER;
        return MembershipLevel.NONE;
    }
}
