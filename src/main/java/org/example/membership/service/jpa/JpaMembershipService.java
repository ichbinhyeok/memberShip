package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JpaMembershipService {
    private final UserRepository userRepository;
    private final org.example.membership.repository.jpa.BadgeRepository badgeRepository;
    private final org.example.membership.repository.jpa.CouponIssueLogRepository couponIssueLogRepository;

    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());

        // 명시적으로 null 체크 → 기본값 유지
        if (request.getMembershipLevel() != null) {
            user.setMembershipLevel(request.getMembershipLevel());
        }
        return userRepository.save(user);
    }



    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
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
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByMembershipLevel(MembershipLevel level) {
        return userRepository.findByMembershipLevel(level);
    }

    @Transactional(readOnly = true)
    public org.example.membership.dto.UserStatusResponse getUserStatus(Long userId) {
        User user = getUserById(userId);
        org.example.membership.dto.UserStatusResponse resp = new org.example.membership.dto.UserStatusResponse();
        resp.setUserId(user.getId());
        resp.setMembershipLevel(user.getMembershipLevel());
        java.util.List<String> badges = badgeRepository.findByUser(user).stream()
                .map(b -> b.getCategory().getName().name())
                .toList();
        resp.setBadges(badges);
        return resp;
    }

    @Transactional(readOnly = true)
    public java.util.List<org.example.membership.dto.CouponInfoResponse> getUserCoupons(Long userId) {
        User user = getUserById(userId);
        return couponIssueLogRepository.findByUser(user).stream().map(log -> {
            org.example.membership.dto.CouponInfoResponse dto = new org.example.membership.dto.CouponInfoResponse();
            dto.setId(log.getCoupon().getId());
            dto.setCode(log.getCoupon().getCode());
            if (log.getCoupon().getCategory() != null) {
                dto.setCategory(log.getCoupon().getCategory().getName().name());
            }
            dto.setExpiresAt(log.getCoupon().getExpiresAt());
            return dto;
        }).toList();
    }
}
