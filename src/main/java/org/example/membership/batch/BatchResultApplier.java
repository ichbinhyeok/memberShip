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

    @Transactional
    public void applyResults(UUID executionId, long startUserId, long endUserId) {
        log.info("[결과 반영 시작] executionId={}, range=[{}~{}]", executionId, startUserId, endUserId);
        applyBadgeResults(executionId, startUserId, endUserId);
        applyLevelResults(executionId, startUserId, endUserId);
        log.info("[결과 반영 완료] executionId={}", executionId);
    }


    public void applyBadgeResults(UUID executionId, long startUserId, long endUserId) {
        List<BadgeResult> pendingBadges =
                badgeResultRepository.findByExecutionIdAndStatusAndUserIdBetween(
                        executionId, BatchResultStatus.PENDING, startUserId, endUserId);

        if (pendingBadges.isEmpty()) {
            log.info("[배지 결과] 반영할 내용 없음. executionId={}, range=[{}~{}]",
                    executionId, startUserId, endUserId);
            return;
        }

        log.info("[배지 결과] {}건 반영 시작 executionId={}, range=[{}~{}]",
                pendingBadges.size(), executionId, startUserId, endUserId);

        for (BadgeResult result : pendingBadges) {
            // 배지 엔티티 조회
            var opt = badgeRepository.findByUserIdAndCategoryId(result.getUserId(), result.getCategoryId());
            if (opt.isEmpty()) {
                // 지금은 생성하지 않고 실패로 마킹(불완전 엔티티 방지)
                log.error("[배지 반영 실패] 배지 없음 userId={}, categoryId={}, executionId={}",
                        result.getUserId(), result.getCategoryId(), executionId);
                result.setStatus(BatchResultStatus.FAILED);
                badgeResultRepository.save(result);
                continue;
            }

            Badge badge = opt.get();
            if (result.isNewState()) badge.activate(); else badge.deactivate();
            badgeRepository.save(badge);

            result.setStatus(BatchResultStatus.APPLIED);
            badgeResultRepository.save(result);
        }
        log.info("[배지 결과] 반영 완료. {}건 APPLIED", pendingBadges.size());
    }

    public void applyLevelResults(UUID executionId, long startUserId, long endUserId) {
        List<LevelResult> pendingLevels =
                levelResultRepository.findByExecutionIdAndStatusAndUserIdBetween(
                        executionId, BatchResultStatus.PENDING, startUserId, endUserId);

        if (pendingLevels.isEmpty()) {
            log.info("[등급 결과] 반영할 내용 없음. executionId={}, range=[{}~{}]",
                    executionId, startUserId, endUserId);
            return;
        }

        log.info("[등급 결과] {}건 반영 시작... executionId={}, range=[{}~{}]",
                pendingLevels.size(), executionId, startUserId, endUserId);

        for (LevelResult result : pendingLevels) {
            User user = userRepository.findById(result.getUserId()).orElse(null);
            if (user == null) {
                log.error("[등급 반영 실패] 사용자 없음 userId={}, executionId={}", result.getUserId(), executionId);
                result.setStatus(BatchResultStatus.FAILED);
                levelResultRepository.save(result);
                continue;
            }

            user.setMembershipLevel(result.getNewLevel());
            user.setLastMembershipChange(LocalDateTime.now());
            userRepository.save(user);

            result.setStatus(BatchResultStatus.APPLIED);
            levelResultRepository.save(result);
        }
        log.info("[등급 결과] 반영 완료. {}건 APPLIED", pendingLevels.size());
    }
}
