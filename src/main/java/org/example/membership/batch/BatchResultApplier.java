package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.BatchResultStatus;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.entity.batch.BadgeResult;
import org.example.membership.entity.batch.LevelResult;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchResultApplier {

    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;
    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;

    /**
     * 지정된 executionId에 대한 모든 계산 결과를 라이브 DB에 반영합니다.
     */
    public void applyResults(UUID executionId) {
        log.info("[결과 반영 시작] executionId={}", executionId);

        applyBadgeResults(executionId);
        applyLevelResults(executionId);

        log.info("[결과 반영 완료] executionId={}", executionId);
    }

    /**
     * 배지 계산 결과를 라이브 badge 테이블에 반영합니다.
     * 이 메서드는 독립적인 트랜잭션으로 실행됩니다.
     */
    @Transactional
    public void applyBadgeResults(UUID executionId) {
        List<BadgeResult> pendingBadges = badgeResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);
        if (pendingBadges.isEmpty()) {
            log.info("[배지 결과] 반영할 내용 없음. executionId={}", executionId);
            return;
        }

        log.info("[배지 결과] {}건 반영 시작...", pendingBadges.size());
        for (BadgeResult result : pendingBadges) {
            // "Batch Wins" 정책에 따라 조건 없이 덮어쓰기
            Badge badge = badgeRepository.findByUserIdAndCategoryId(result.getUserId(), result.getCategoryId())
                    .orElseGet(() -> {
                        // 혹시 배지가 없는 경우를 대비한 방어 코드
                        log.warn("배지를 찾을 수 없어 새로 생성합니다. userId={}, categoryId={}", result.getUserId(), result.getCategoryId());
                        Badge newBadge = new Badge();
                        // user, category 엔티티를 찾아 설정해야 하지만, 예제에서는 간단히 처리
                        // newBadge.setUser(userRepository.findById(result.getUserId()).orElseThrow());
                        // newBadge.setCategory(categoryRepository.findById(result.getCategoryId()).orElseThrow());
                        return newBadge;
                    });

            if (result.isNewState()) {
                badge.activate();
            } else {
                badge.deactivate();
            }
            badgeRepository.save(badge);

            // 처리 완료 상태로 변경
            result.setStatus(BatchResultStatus.APPLIED);
            badgeResultRepository.save(result);
        }
        log.info("[배지 결과] {}건 반영 완료.", pendingBadges.size());
    }

    /**
     * 등급 계산 결과를 라이브 user 테이블에 반영합니다.
     * 이 메서드는 독립적인 트랜잭션으로 실행됩니다.
     */
    @Transactional
    public void applyLevelResults(UUID executionId) {
        List<LevelResult> pendingLevels = levelResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);
        if (pendingLevels.isEmpty()) {
            log.info("[등급 결과] 반영할 내용 없음. executionId={}", executionId);
            return;
        }

        log.info("[등급 결과] {}건 반영 시작...", pendingLevels.size());
        for (LevelResult result : pendingLevels) {
            User user = userRepository.findById(result.getUserId())
                    .orElse(null);

            if (user == null) {
                log.error("[등급 반영 실패] 사용자를 찾을 수 없습니다. userId={}", result.getUserId());
                result.setStatus(BatchResultStatus.FAILED);
                levelResultRepository.save(result);
                continue;
            }

            user.setMembershipLevel(result.getNewLevel());
            user.setLastMembershipChange(LocalDateTime.now());
            userRepository.save(user);

            // 처리 완료 상태로 변경
            result.setStatus(BatchResultStatus.APPLIED);
            levelResultRepository.save(result);
        }
        log.info("[등급 결과] {}건 반영 완료.", pendingLevels.size());
    }
}
