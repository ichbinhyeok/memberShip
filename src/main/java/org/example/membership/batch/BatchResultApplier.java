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
     * 지정 executionId의 배지/레벨 결과를 DB에 반영한다.
     */
    @Transactional
    public void applyResults(UUID executionId) {
        applyBadgeResults(executionId);
        applyLevelResults(executionId);
    }

    /**
     * 배지 반영 로직
     */
    private void applyBadgeResults(UUID executionId) {
        List<BadgeResult> badgeResults =
                badgeResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);

        for (BadgeResult result : badgeResults) {
            try {

                Badge badge = badgeRepository.findByUserId(result.getUserId())
                        .orElseThrow(() -> new IllegalStateException("Badge not found for userId=" + result.getUserId()));



                // 배치 산출물 기반 반영
                badge.applyFromResult(result);

                badgeRepository.save(badge);

                // 결과 상태 업데이트
                result.setStatus(BatchResultStatus.APPLIED);
                result.setAppliedAt(LocalDateTime.now());

            } catch (Exception e) {
                result.setStatus(BatchResultStatus.FAILED);
                log.error("배지 반영 실패 userId={}", result.getUserId(), e);
            }
        }

        badgeResultRepository.saveAll(badgeResults);
    }

    /**
     * 레벨 반영 로직
     */
    private void applyLevelResults(UUID executionId) {
        List<LevelResult> levelResults =
                levelResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);

        for (LevelResult result : levelResults) {
            try {
                // 유저 조회
                User user = userRepository.findById(result.getUserId())
                        .orElseThrow(() -> new IllegalStateException("User not found: " + result.getUserId()));

                // 배치 산출물 기반 레벨 반영
                user.applyLevelFromResult(result);

                userRepository.save(user);

                // 결과 상태 업데이트
                result.setStatus(BatchResultStatus.APPLIED);
                result.setAppliedAt(LocalDateTime.now());

            } catch (Exception e) {
                result.setStatus(BatchResultStatus.FAILED);
                log.error("레벨 반영 실패 userId={}", result.getUserId(), e);
            }
        }

        levelResultRepository.saveAll(levelResults);
    }
}
