package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.BatchResultStatus;
import org.example.membership.entity.Badge;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.entity.batch.BadgeResult;
import org.example.membership.entity.batch.LevelResult;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.MembershipLogRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final MembershipLogRepository membershipLogRepository;

    @Transactional
    public void applyResults(UUID executionId, LocalDateTime batchStartTime) {
        log.info("[배치 결과 일괄 적용 시작] executionId={}", executionId);
        long start = System.currentTimeMillis();

        applyBadgeResults(executionId, batchStartTime);
        applyLevelResults(executionId, batchStartTime);

        long duration = System.currentTimeMillis() - start;
        log.info("[배치 결과 일괄 적용 완료] executionId={}, 소요 시간: {}ms", executionId, duration);
    }

    /**
     */
    public void applyBadgeResults(UUID executionId, LocalDateTime batchStartTime) {
        log.info("[배지 결과 적용 시작] executionId={}", executionId);
        long start = System.currentTimeMillis();

        List<BadgeResult> badgeResults =
                badgeResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);
        if (badgeResults.isEmpty()) {
            log.info("[배지 결과 적용] 처리할 대상 없음");
            return;
        }

        List<Long> userIds = badgeResults.stream().map(BadgeResult::getUserId).toList();
        Map<String, Badge> badgeMap = badgeRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        badge -> badge.getUser().getId() + ":" + badge.getCategory().getId(),
                        Function.identity()
                ));

        List<Badge> badgesToUpdate = new ArrayList<>();

        for (BadgeResult result : badgeResults) {
            try {
                String key = result.getUserId() + ":" + result.getCategoryId();
                Badge badge = badgeMap.get(key);
                if (badge == null) continue;

                // 자바 코드 내에서 조건부 업데이트 로직 실행
                if (badge.getUpdatedAt() == null || badge.getUpdatedAt().isBefore(batchStartTime)) {
                    badge.applyFromResult(result);
                    badgesToUpdate.add(badge); // 변경된 Badge만 저장 대상에 추가
                }

                result.setStatus(BatchResultStatus.APPLIED);
                result.setAppliedAt(LocalDateTime.now());
            } catch (Exception e) {
                result.setStatus(BatchResultStatus.FAILED);
                log.error("배지 반영 실패 userId={}, categoryId={}", result.getUserId(), result.getCategoryId(), e);
            }
        }

        if (!badgesToUpdate.isEmpty()) {
            log.info("[badges 테이블 업데이트 시작] 대상: {}건", badgesToUpdate.size());
            badgeRepository.saveAll(badgesToUpdate);
            log.info("[badges 테이블 업데이트 완료]");
        }

        log.info("[badge_results 테이블 상태 업데이트 시작] 대상: {}건", badgeResults.size());
        badgeResultRepository.saveAll(badgeResults);
        log.info("[badge_results 테이블 상태 업데이트 완료]");

        long duration = System.currentTimeMillis() - start;
        log.info("[배지 결과 적용 완료] executionId={}, 소요 시간: {}ms", executionId, duration);
    }
    /**
     * 등급 업데이트: 로그 생성을 위해 엔티티를 조회하고 JPA 변경 감지(Dirty Checking)를 활용
     */
    public void applyLevelResults(UUID executionId, LocalDateTime batchStartTime) {
        log.info("[레벨 결과 적용 시작] executionId={}", executionId);
        long start = System.currentTimeMillis();

        List<LevelResult> levelResults =
                levelResultRepository.findByExecutionIdAndStatus(executionId, BatchResultStatus.PENDING);
        if (levelResults.isEmpty()) {
            log.info("[레벨 결과 적용] 처리할 대상 없음");
            return;
        }

        List<Long> userIds = levelResults.stream().map(LevelResult::getUserId).toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<MembershipLog> logsToSave = new ArrayList<>();
        List<User> usersToUpdate = new ArrayList<>();

        for (LevelResult result : levelResults) {
            try {
                User user = userMap.get(result.getUserId());
                if (user == null) continue;

                //  자바 코드 내에서 조건부 업데이트 로직 실행
                if (user.getLastMembershipChange() == null || user.getLastMembershipChange().isBefore(batchStartTime)) {
                    MembershipLog log = user.recordLevelChange(result.getNewLevel(), "월간 배치 실행");
                    if (log != null) {
                        logsToSave.add(log);
                    }
                    user.applyLevelFromResult(result);
                    usersToUpdate.add(user); // 변경된 User만 저장 대상에 추가
                }
                result.setStatus(BatchResultStatus.APPLIED);
                result.setAppliedAt(LocalDateTime.now());
            } catch (Exception e) {
                result.setStatus(BatchResultStatus.FAILED);
                log.error("레벨 반영 실패 userId={}", result.getUserId(), e);
            }
        }

        if (!usersToUpdate.isEmpty()) {
            log.info("[users 테이블 업데이트 시작] 대상: {}건", usersToUpdate.size());
            userRepository.saveAll(usersToUpdate);
            log.info("[users 테이블 업데이트 완료]");
        }
        if (!logsToSave.isEmpty()) {
            log.info("[membership_log 테이블 저장 시작] 대상: {}건", logsToSave.size());
            membershipLogRepository.saveAll(logsToSave);
            log.info("[membership_log 테이블 저장 완료]");
        }

        log.info("[level_results 테이블 상태 업데이트 시작] 대상: {}건", levelResults.size());
        levelResultRepository.saveAll(levelResults);
        long duration = System.currentTimeMillis() - start;
        log.info("[레벨 결과 적용 완료] executionId={}, 소요 시간: {}ms", executionId, duration);
    }
}