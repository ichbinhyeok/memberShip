package org.example.membership.service.pipeline;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.example.membership.entity.Badge;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.example.membership.repository.mybatis.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipServiceMyBatis {

    private final UserMapper userMapper;
    private final BadgeMapper badgeMapper;

    @Transactional
    public void updateUserLevels(List<User> users) {
        userMapper.bulkUpdateMembershipLevels(users);
    }

    @Transactional
    public MembershipLevel updateUserLevel(User user) {
        long activeCount = badgeMapper.findByUserId(user.getId())
                .stream()
                .filter(Badge::isActive)
                .count();
        MembershipLevel prev = user.getMembershipLevel();
        MembershipLevel newLevel = calculateLevel(activeCount);
        user.setMembershipLevel(newLevel);
        user.setLastMembershipChange(java.time.LocalDateTime.now());
        userMapper.update(user);
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
