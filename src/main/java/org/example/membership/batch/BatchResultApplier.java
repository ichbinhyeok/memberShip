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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchResultApplier {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;

    // ✨ 새로 만든 트랜잭션 처리 클래스를 주입받습니다.
    private final TransactionalChunkProcessor chunkProcessor;

    // ✨ 메소드 레벨의 @Transactional은 제거합니다.
    public void applyResults(UUID executionId, LocalDateTime batchStartTime) {
        log.info("[배치 결과 일괄 적용 시작] executionId={}", executionId);
        applyBadgeResults(executionId, batchStartTime);
        applyLevelResults(executionId, batchStartTime);
        log.info("[배치 결과 일괄 적용 완료] executionId={}", executionId);
    }

    public void applyBadgeResults(UUID executionId, LocalDateTime batchStartTime) {
        log.info("[배지 결과 적용 시작] executionId={}", executionId);
        final int CHUNK_SIZE = 1000;

        // 1. 모든 처리 대상을 한 번에 조회
        List<BadgeResult> allBadgeResults =
                badgeResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);
        if (allBadgeResults.isEmpty()) {
            log.info("[배지 결과 적용] 처리할 대상 없음");
            return;
        }

        List<Long> userIds = allBadgeResults.stream().map(BadgeResult::getUserId).distinct().toList();
        Map<String, Badge> badgeMap = badgeRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(b -> b.getUser().getId() + ":" + b.getCategory().getId(), Function.identity()));

        // 2. 전체 대상을 청크 단위로 나누어 처리
        for (int i = 0; i < allBadgeResults.size(); i += CHUNK_SIZE) {
            List<BadgeResult> resultChunk = allBadgeResults.subList(i, Math.min(i + CHUNK_SIZE, allBadgeResults.size()));

            List<Badge> badgesToUpdateInChunk = new ArrayList<>();
            for (BadgeResult result : resultChunk) {
                Badge badge = badgeMap.get(result.getUserId() + ":" + result.getCategoryId());
                if (badge != null && (badge.getUpdatedAt() == null || badge.getUpdatedAt().isBefore(batchStartTime))) {
                    badge.applyFromResult(result);
                    badgesToUpdateInChunk.add(badge);
                }
                result.setStatus(BatchResultStatus.APPLIED);
                result.setAppliedAt(LocalDateTime.now());
            }

            // 3. 분리된 클래스의 @Transactional 메소드를 호출하여 청크 처리
            chunkProcessor.applyBadgeChunk(badgesToUpdateInChunk, resultChunk);
        }
        log.info("[배지 결과 적용 완료] executionId={}", executionId);
    }

    public void applyLevelResults(UUID executionId, LocalDateTime batchStartTime) {
        log.info("[레벨 결과 적용 시작] executionId={}", executionId);
        final int CHUNK_SIZE = 1000;

        // 1. 모든 처리 대상을 한 번에 조회
        List<LevelResult> allLevelResults =
                levelResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);
        if (allLevelResults.isEmpty()) {
            log.info("[레벨 결과 적용] 처리할 대상 없음");
            return;
        }

        List<Long> userIds = allLevelResults.stream().map(LevelResult::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 2. 전체 대상을 청크 단위로 나누어 처리
        for (int i = 0; i < allLevelResults.size(); i += CHUNK_SIZE) {
            List<LevelResult> resultChunk = allLevelResults.subList(i, Math.min(i + CHUNK_SIZE, allLevelResults.size()));

            List<User> usersToUpdateInChunk = new ArrayList<>();
            for (LevelResult result : resultChunk) {
                User user = userMap.get(result.getUserId());
                if (user != null && (user.getLastMembershipChange() == null || user.getLastMembershipChange().isBefore(batchStartTime))) {
                    user.applyLevelAndLog(result, "월간 배치 실행");
                    usersToUpdateInChunk.add(user);
                }
                result.setStatus(BatchResultStatus.APPLIED);
                result.setAppliedAt(LocalDateTime.now());
            }

            // 3. 분리된 클래스의 @Transactional 메소드를 호출하여 청크 처리
            chunkProcessor.applyLevelChunk(usersToUpdateInChunk, resultChunk);
        }
        log.info("[레벨 결과 적용 완료] executionId={}", executionId);
    }
}