package org.example.membership.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.dto.UserStatusResponse;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Category;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.exception.NotFoundException;
import org.example.membership.repository.jpa.*;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JpaMembershipService {
    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final CouponIssueLogRepository couponIssueLogRepository;
    private final CategoryRepository categoryRepository;
    private final MembershipLogRepository membershipLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());

        if (request.getMembershipLevel() != null) {
            user.setMembershipLevel(request.getMembershipLevel());
        }
        user = userRepository.save(user);

        List<Category> categories = categoryRepository.findAll();
        List<Badge> badges = new ArrayList<>();
        for (Category category : categories) {
            Badge badge = new Badge();
            badge.setUser(user);
            badge.setCategory(category);
            badge.setActive(false);
            badges.add(badge);
        }
        badgeRepository.saveAll(badges);
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public MembershipInfoResponse getUserByName(String name) {
        User user = userRepository.findByName(name).orElseThrow(() -> new NotFoundException("User not found"));
        return MembershipInfoResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateMembershipLevel(Long userId, MembershipLevel newLevel) {
        User user = getUserById(userId);
        user.setMembershipLevel(newLevel);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        userRepository.delete(user);
    }
    @Transactional(readOnly = true)
    public List<User> getUsersByMembershipLevel(MembershipLevel level) {
        return userRepository.findByMembershipLevel(level);
    }

    @Transactional(readOnly = true)
    public UserStatusResponse getUserStatus(Long userId) {
        User user = getUserById(userId);
        UserStatusResponse resp = new org.example.membership.dto.UserStatusResponse();
        resp.setUserId(user.getId());
        resp.setMembershipLevel(user.getMembershipLevel());
        List<String> badges = badgeRepository.findByUser(user).stream()
                .map(b -> b.getCategory().getName().name())
                .toList();
        resp.setBadges(badges);
        return resp;
    }

    public MembershipLevel calculateLevel(long badgeCount) {
        if (badgeCount >= 3) return MembershipLevel.VIP;
        if (badgeCount == 2) return MembershipLevel.GOLD;
        if (badgeCount == 1) return MembershipLevel.SILVER;
        return MembershipLevel.NONE;
    }

    @Transactional
    public void bulkUpdateMembershipLevelsAndLog(List<User> users,
                                                 Map<Long, Long> activeBadgeMap,
                                                 int batchSize) {
        Map<Long, MembershipLevel> prevLevelMap = new HashMap<>();
        int count = 0;

        // 1차 루프: 등급 갱신 및 이전 등급 캐싱
        for (User user : users) {
            long badgeCount = activeBadgeMap.getOrDefault(user.getId(), 0L);
            MembershipLevel prev = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(badgeCount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            prevLevelMap.put(user.getId(), prev);

            count++;
            flushAndClearIfNeeded(count, batchSize);
        }

        entityManager.flush();
        entityManager.clear();

        // 2차 루프: 로그 삽입
        count = 0;
        for (User user : users) {
            MembershipLevel prev = prevLevelMap.get(user.getId());
            MembershipLevel current = user.getMembershipLevel();
            long badgeCount = activeBadgeMap.getOrDefault(user.getId(), 0L);

            MembershipLog log = new MembershipLog();
            log.setUser(user);
            log.setPreviousLevel(prev);
            log.setNewLevel(current);
            log.setChangeReason("badge count: " + badgeCount);

            entityManager.persist(log);

            count++;
            flushAndClearIfNeeded(count, batchSize);
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Transactional
    public MembershipLog manualChangeLevel(Long userId, MembershipLevel newLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getMembershipLevel() == newLevel) {
            return null;
        }

        MembershipLevel previous = user.getMembershipLevel();
        user.setMembershipLevel(newLevel);
        user.setLastMembershipChange(LocalDateTime.now());

        MembershipLog log = new MembershipLog();
        log.setUser(user);
        log.setPreviousLevel(previous);
        log.setNewLevel(newLevel);
        log.setChangeReason("manual update");

        membershipLogRepository.save(log);

        return log;
    }



    private void flushAndClearIfNeeded(int count, int batchSize) {
        if (count % batchSize == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}
