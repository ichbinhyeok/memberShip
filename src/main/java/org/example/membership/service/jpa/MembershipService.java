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
