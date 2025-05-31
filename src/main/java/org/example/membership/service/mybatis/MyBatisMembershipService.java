package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.domain.user.User;
import org.example.membership.domain.user.UserMapper;
import org.example.membership.domain.user.MembershipLevel;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyBatisMembershipService {
    private final UserMapper userMapper;

    @Transactional
    public User createUser(User user) {
        userMapper.insert(user);
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
} 