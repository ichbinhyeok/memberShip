package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.UserMapper;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyBatisMembershipService {
    private final UserMapper userMapper;

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
} 