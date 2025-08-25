package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.entity.batch.BadgeResult;
import org.example.membership.entity.batch.LevelResult;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TransactionalChunkProcessor {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyBadgeChunk(List<Badge> badgeChunk, List<BadgeResult> badgeResultChunk) {
        if (!badgeChunk.isEmpty()) {
            badgeRepository.saveAll(badgeChunk);
        }
        if (!badgeResultChunk.isEmpty()) {
            badgeResultRepository.saveAll(badgeResultChunk);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyBadgeChunkBulk(List<UUID> resultIds) {
        badgeRepository.applyFromResults(resultIds);   // 1) 조인 업데이트
        badgeResultRepository.markApplied(resultIds);  // 2) 결과 상태 일괄 반영
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyLevelChunk(List<User> userChunk, List<LevelResult> levelResultChunk) {
        if (!userChunk.isEmpty()) {
            userRepository.saveAll(userChunk);
        }
        if (!levelResultChunk.isEmpty()) {
            levelResultRepository.saveAll(levelResultChunk);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyLevelChunkManaged(List<UUID> resultIds, LocalDateTime t0) {
        // 이 트랜잭션 안에서 "다시" 로드하여 managed 상태로 만든다
        List<LevelResult> results = levelResultRepository.findAllById(resultIds);
        List<Long> userIds = results.stream().map(LevelResult::getUserId).distinct().toList();
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        for (LevelResult r : results) {
            User u = users.get(r.getUserId());
            if (u != null && (u.getLastMembershipChange()==null || u.getLastMembershipChange().isBefore(t0))) {
                u.applyLevelAndLog(r, "월간 배치 실행"); // 도메인 로직/로그 보존
            }
            r.markApplied();
        }
        // 커밋 시 더티체크로 배치 UPDATE 발생 (hibernate.jdbc.batch_size 적용)
    }


}
