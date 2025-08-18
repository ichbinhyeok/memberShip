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

@Component
@RequiredArgsConstructor
@Slf4j
public class LevelResultCalculator {

    private final LevelResultRepository levelResultRepository;
    private final BadgeRepository badgeRepository;

    /**
     * 사용자 리스트를 받아, 새로운 멤버십 등급을 계산하고 level_results 테이블에 저장합니다.
     * @param executionId 현재 배치의 실행 ID
     * @param users 처리 대상 사용자 리스트
     * @param batchSize 한번에 저장할 배치 크기
     */
    public void calculateAndStoreResults(UUID executionId, List<User> users, int batchSize, LocalDateTime batchStartTime) {
        if (CollectionUtils.isEmpty(users)) {
            log.warn("[등급 계산 스킵] 대상 없음. executionId={}", executionId);
            return;
        }

        log.info("[등급 결과 계산 시작] 대상: {}건, executionId={}", users.size(), executionId);

        // batchStartTime을 전달하여 T0 시점의 배지 개수를 조회
        Map<Long, Long> userBadgeCountMap = getActiveBadgeCountMapAsOfT0(users, batchStartTime);

        // 2. 각 사용자의 새로운 등급을 계산하여 LevelResult 리스트 생성
        List<LevelResult> results = new ArrayList<>();
        for (User user : users) {
            long badgeCount = userBadgeCountMap.getOrDefault(user.getId(), 0L);
            MembershipLevel newLevel = determineNewLevel(badgeCount);

            // 등급에 변경이 있을 경우에만 결과 테이블에 저장
            if (user.getMembershipLevel() != newLevel) {
                results.add(LevelResult.builder()
                        .executionId(executionId)
                        .userId(user.getId())
                        .newLevel(newLevel)
                        .build());
            }
        }

        // 3. JpaRepository의 saveAll을 통해 배치 저장
        if (!results.isEmpty()) {
            levelResultRepository.saveAll(results);
            log.info("[등급 결과 계산 및 저장 완료] {}건의 등급 변경 처리", results.size());
        } else {
            log.info("[등급 결과 계산 완료] 등급 변경 대상 없음");
        }
    }

    /**
     * 사용자 목록을 받아,T0시점 각 사용자의 활성 배지 개수를 Map 형태로 반환합니다.
     */
    private Map<Long, Long> getActiveBadgeCountMapAsOfT0(List<User> users, LocalDateTime batchStartTime) {
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        List<Object[]> counts = badgeRepository.countActiveBadgesAsOfT0(userIds, batchStartTime);

        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : counts) {
            Long userId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            map.put(userId, count);
        }
        return map;
    }

    /**
     * 활성 배지 개수에 따라 새로운 멤버십 등급을 결정합니다.
     * (비즈니스 로직)
     */
    private MembershipLevel determineNewLevel(long activeBadgeCount) {
        if (activeBadgeCount >= 6) {
            return MembershipLevel.VIP;
        } else if (activeBadgeCount >= 4) {
            return MembershipLevel.GOLD;
        } else if (activeBadgeCount >= 2) {
            return MembershipLevel.SILVER;
        } else {
            return MembershipLevel.NONE;
        }
    }
}
