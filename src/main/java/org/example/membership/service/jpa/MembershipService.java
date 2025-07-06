package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public MembershipLevel updateUserLevel(User user) {
        long activeCount = badgeRepository.countByUserAndActiveTrue(user);
        MembershipLevel prev = user.getMembershipLevel();
        MembershipLevel newLevel = calculateLevel(activeCount);
        user.setMembershipLevel(newLevel);
        user.setLastMembershipChange(LocalDateTime.now());
        userRepository.save(user);
        return prev;
    }

    /**
     * 대량 처리 시 badge count를 외부에서 주입받아 등급을 갱신함
     * 쿼리 부하를 방지하고 성능을 최적화하기 위한 전용 메서드
     */
    @Transactional
    public MembershipLevel updateUserLevelWithBadgeCount(User user, long badgeCount) {
        MembershipLevel prev = user.getMembershipLevel();
        MembershipLevel newLevel = calculateLevel(badgeCount);
        user.setMembershipLevel(newLevel);
        user.setLastMembershipChange(LocalDateTime.now());
        return prev;
    }

    @Transactional
    public void bulkUpdateMembershipLevels() {
        List<User> users = userRepository.findAll();

        Map<Long, Long> activeBadgeMap = badgeRepository.countActiveBadgesGroupedByUserId()
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        final int BATCH_SIZE = 1000;
        int count = 0;

        for (User user : users) {
            long badgeCount = activeBadgeMap.getOrDefault(user.getId(), 0L);
            MembershipLevel newLevel = calculateLevel(badgeCount);
            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            userRepository.save(user);

            count++;
            flushAndClearIfNeeded(count, BATCH_SIZE);
        }

        entityManager.flush();
        entityManager.clear();
    }



    public MembershipLevel calculateLevel(long badgeCount) {
        if (badgeCount >= 3) {
            return MembershipLevel.VIP;
        } else if (badgeCount == 2) {
            return MembershipLevel.GOLD;
        } else if (badgeCount == 1) {
            return MembershipLevel.SILVER;
        }
        return MembershipLevel.NONE;
    }

    private void flushAndClearIfNeeded(int count, int batchSize) {
        if (count % batchSize == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}
