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

import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionalChunkProcessor {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;

    /**
     * 배지 관련 청크 데이터를 하나의 새 트랜잭션으로 처리합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyBadgeChunk(List<Badge> badgeChunk, List<BadgeResult> badgeResultChunk) {
        if (!badgeChunk.isEmpty()) {
            badgeRepository.saveAll(badgeChunk);
        }
        badgeResultRepository.saveAll(badgeResultChunk);
    }

    /**
     * 레벨 관련 청크 데이터를 하나의 새 트랜잭션으로 처리합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyLevelChunk(List<User> userChunk, List<LevelResult> levelResultChunk) {
        if (!userChunk.isEmpty()) {
            userRepository.saveAll(userChunk);
        }
        levelResultRepository.saveAll(levelResultChunk);
    }
}