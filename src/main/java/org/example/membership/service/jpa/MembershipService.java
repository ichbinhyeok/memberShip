package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;

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
}
