package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.MembershipLogMapper;
import org.example.membership.repository.mybatis.UserMapper;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyBatisMembershipService {
    private final UserMapper userMapper;
    private final MembershipLogMapper membershipLogMapper;

    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());

        // 기본값은 SILVER, null 방어
        if (request.getMembershipLevel() != null) {
            user.setMembershipLevel(request.getMembershipLevel());
        }

        user.setCreatedAt(LocalDateTime.now()); // MyBatis는 직접 세팅 필요
        userMapper.insert(user); // id 자동 주입됨
        return user;
    }


    @Transactional(readOnly = true)
    public MembershipInfoResponse getUserById(Long id) {
        MembershipInfoResponse response = userMapper.selectMemberShipInfo(id);
        if (response == null) {
            throw new NotFoundException("User not found");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public MembershipInfoResponse getUserByUsername(String username) {
        MembershipInfoResponse response = userMapper.selectMemberShipInfoByName(username);
        if (response == null) {
            throw new NotFoundException("User not found");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userMapper.findAll();
    }

    @Transactional
    public User updateMembershipLevel(Long userId, MembershipLevel newLevel) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        user.setMembershipLevel(newLevel);
        userMapper.update(user);
        return user;
    }

    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByMembershipLevel(MembershipLevel level) {
        return userMapper.findByMembershipLevel(level);
    }
    @Transactional
    public void bulkUpdateMembershipLevelsAndLog(List<User> users,
                                                 Map<Long, Long> activeBadgeCountMap,
                                                 int batchSize) {
        int count = 0;

        for (User user : users) {
            long badgeCount = activeBadgeCountMap.getOrDefault(user.getId(), 0L);
            MembershipLevel prev = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(badgeCount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            userMapper.update(user);

            MembershipLog log = new MembershipLog();
            log.setUser(user);
            log.setPreviousLevel(prev);
            log.setNewLevel(newLevel);
            log.setChangeReason("badge count: " + badgeCount);
            membershipLogMapper.insert(log);

            count++;
            if (count % batchSize == 0) {
                System.out.println("Flushed " + count + " user updates");
            }
        }
    }
    public MembershipLevel calculateLevel(long badgeCount) {
        if (badgeCount >= 3) return MembershipLevel.VIP;
        if (badgeCount == 2) return MembershipLevel.GOLD;
        if (badgeCount == 1) return MembershipLevel.SILVER;
        return MembershipLevel.NONE;
    }
} 