package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.batch.BadgeResult;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BadgeResultCalculator {

    private final BadgeResultRepository badgeResultRepository;

    /**
     * 배지 갱신 대상 맵을 받아, 계산 결과를 badge_results 테이블에 저장합니다.
     */
    //  파라미터 타입을 Map<String, Boolean>으로 변경
    public void calculateAndStoreResults(UUID executionId, Map<String, Boolean> keysToUpdate, int batchSize) {
        if (CollectionUtils.isEmpty(keysToUpdate)) {
            log.warn("[배지 계산 스킵] 대상 없음. executionId={}", executionId);
            return;
        }

        log.info("[배지 결과 계산 시작] 대상: {}건, executionId={}", keysToUpdate.size(), executionId);

        List<BadgeResult> results = keysToUpdate.entrySet().stream()
                .map(entry -> parseToBadgeResult(executionId, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        badgeResultRepository.saveAll(results);
        log.info("[배지 결과 계산 및 저장 완료] {}건 처리", results.size());
    }

    private BadgeResult parseToBadgeResult(UUID executionId, String key, boolean newState) {
        //key는 String "userId +":"+categoryId"

        try {
            String[] parts = key.split(":");
            Long userId = Long.parseLong(parts[0]);
            Long categoryId = Long.parseLong(parts[1]);

            return BadgeResult.builder()
                    .executionId(executionId)
                    .userId(userId)
                    .categoryId(categoryId)
                    .newState(newState)
                    .build();
        } catch (Exception e) {
            log.error("[파싱 실패] 잘못된 배지 키 형식입니다. key={}, executionId={}", key, executionId, e);
            throw new IllegalArgumentException("Invalid key format: " + key, e);
        }
    }
}
